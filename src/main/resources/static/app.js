/* Sofra marketplace — every number on screen comes from the live API. */
const FALLBACK_IMG = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1200&q=80&auto=format&fit=crop";
const DISH_FALLBACK = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=400&q=80&auto=format&fit=crop";

/* Hand-picked icon set (single stroke system) — no emoji icons anywhere. */
const I = (paths, vb = "0 0 24 24") =>
  `<svg viewBox="${vb}" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths}</svg>`;
const ICON = {
  pin: I(`<path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/>`),
  clock: I(`<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>`),
  phone: I(`<path d="M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1 1 .4 2 .7 2.9a2 2 0 0 1-.5 2.1L8 10a16 16 0 0 0 6 6l1.3-1.3a2 2 0 0 1 2.1-.5c.9.3 1.9.6 2.9.7a2 2 0 0 1 1.7 2Z"/>`),
  users: I(`<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8"/>`),
  check: I(`<path d="M20 6 9 17l-5-5"/>`),
  cal: I(`<rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/>`),
  nav: I(`<path d="m3 11 19-9-9 19-2-8-8-2Z"/>`),
  book: I(`<path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20V2H6.5A2.5 2.5 0 0 0 4 4.5v15Z"/><path d="M4 19.5A2.5 2.5 0 0 0 6.5 22H20v-5"/>`),
};

/* Arabic house-names, set by hand for the seed venues. */
const AR_NAMES = { "El Saraya Lounge": "السرايا", "Casa Italia": "كازا إيتاليا", "Sushi Bay": "سوشي باي" };

const state = { page: 0, size: 6, q: "", area: "", cuisine: "All", price: "", openOnly: false, topOnly: false, totalPages: 1, cache: new Map(), ratings: new Map() };
const bookingCtx = { date: new Date(Date.now() + 864e5).toISOString().slice(0, 10), party: 1 };
const todayStr = () => new Date().toISOString().slice(0, 10);
const AVATAR_COLORS = ["#bc3f2a", "#a87f24", "#2e7d4f", "#4a5fa5", "#7a4a9e", "#0f766e"];

const $ = (id) => document.getElementById(id);
const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const imgFallback = (fb = FALLBACK_IMG) => `onerror="if(!this.dataset.fbk){this.dataset.fbk=1;this.src='${fb}'}"`;

/* ── api ── */
async function api(path) {
  const res = await fetch(path);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}
const getRestaurants = (q, page, size) => {
  const p = new URLSearchParams({ page, size });
  if (q) p.set("search", q);
  return api(`/api/restaurants?${p}`);
};
const getMenu = (id) => api(`/api/restaurants/${id}/menu`).catch(() => []);
async function getRating(id) {
  if (state.ratings.has(id)) return state.ratings.get(id);
  try {
    const r = await api(`/api/restaurants/${id}/reviews?page=0&size=100`);
    const list = r.content || [];
    const val = { count: list.length, avg: list.length ? list.reduce((a, v) => a + v.rating, 0) / list.length : 0 };
    state.ratings.set(id, val);
    return val;
  } catch { return { count: 0, avg: 0 }; }
}

/* ── hours: "12:00" or "12:00:00" → minutes ── */
function toMin(s, fb) {
  if (!s) return fb;
  const [h, m] = s.split(":").map(Number);
  return (h || 0) * 60 + (m || 0);
}
function isOpenNow(r, at = new Date()) {
  const open = toMin(r.openingTime, 12 * 60), close = toMin(r.closingTime, 23 * 60 + 59);
  const now = at.getHours() * 60 + at.getMinutes();
  return close > open ? now >= open && now < close : now >= open || now < close;
}
function fmtHM(mins) {
  let h = Math.floor(mins / 60) % 24, m = mins % 60;
  const ap = h >= 12 ? "PM" : "AM";
  h = h % 12 || 12;
  return `${h}:${String(m).padStart(2, "0")} ${ap}`;
}
/* Dinner-first slots: prefer the evening inside opening hours, else from opening. */
function venueSlots(r, max = 5) {
  const open = toMin(r.openingTime, 12 * 60), close = toMin(r.closingTime, 23 * 60);
  let start = Math.min(Math.max(18 * 60, open), Math.max(open, close - 60));
  const slots = [];
  for (let t = start; t < close && slots.length < max; t += 60) slots.push(t);
  if (!slots.length) for (let t = open; t < close && slots.length < max; t += 60) slots.push(t);
  return slots.length ? slots : [19 * 60, 20 * 60, 21 * 60];
}
function allDaySlots(r) {
  const open = toMin(r.openingTime, 12 * 60), close = toMin(r.closingTime, 23 * 60);
  const out = [];
  for (let t = open; t < close; t += 60) out.push(t);
  return out.length ? out : [19 * 60, 20 * 60];
}
const fmtSlot = (mins) => fmtHM(mins);
const slotVal = (mins) => `${String(Math.floor(mins / 60) % 24).padStart(2, "0")}:${String(mins % 60).padStart(2, "0")}`;

function stars(avg) {
  let out = "";
  for (let i = 1; i <= 5; i++) out += `<span class="${i <= Math.round(avg) ? "" : "off"}">★</span>`;
  return `<span class="stars" aria-label="${avg.toFixed(1)} out of 5">${out}</span>`;
}
const avatarColor = (name) => AVATAR_COLORS[[...name].reduce((a, c) => a + c.charCodeAt(0), 0) % AVATAR_COLORS.length];
const initials = (name) => name.split(/\s+/).map((w) => w[0]).join("").slice(0, 2).toUpperCase();
const areaOf = (r) => (r.location || "").split("-").pop()?.trim() || r.location || "";

/* ── toast / modal ── */
let toastT;
function toast(msg) {
  const t = $("toast");
  t.textContent = msg;
  t.classList.remove("hidden");
  clearTimeout(toastT);
  toastT = setTimeout(() => t.classList.add("hidden"), 2600);
}
function openCmd(tableId, date, slotMins, party) {
  const start = new Date(`${date}T${slotVal(slotMins)}:00`).toISOString();
  const end = new Date(new Date(start).getTime() + 2 * 3600e3).toISOString();
  $("cmd-text").textContent =
`curl -X POST http://localhost:8080/api/reservations \\
  -H "Authorization: Bearer <YOUR_JWT>" \\
  -H "Content-Type: application/json" \\
  -d '{"tableId":${tableId},"startTime":"${start}","endTime":"${end}","numberOfGuests":${party}}'`;
  $("cmd-modal").classList.remove("hidden");
}
$("cmd-close").onclick = () => $("cmd-modal").classList.add("hidden");
$("cmd-modal").addEventListener("click", (e) => { if (e.target.id === "cmd-modal") $("cmd-modal").classList.add("hidden"); });
document.addEventListener("keydown", (e) => { if (e.key === "Escape") $("cmd-modal").classList.add("hidden"); });
$("cmd-copy").onclick = async () => {
  try { await navigator.clipboard.writeText($("cmd-text").textContent); toast("Command copied. Add your JWT and run it."); }
  catch { toast("Select the text and copy it manually."); }
};

/* ── reveal on scroll (subtle, once) ── */
const revealIO = new IntersectionObserver((entries) => {
  entries.forEach((e) => { if (e.isIntersecting) { e.target.classList.add("in"); revealIO.unobserve(e.target); } });
}, { threshold: 0.12 });
const reveal = (el) => { el.classList.add("reveal"); revealIO.observe(el); };

/* ── nav ── */
window.addEventListener("scroll", () => $("nav").classList.toggle("scrolled", scrollY > 8), { passive: true });
document.querySelectorAll("[data-nav='home']").forEach((a) =>
  a.addEventListener("click", () => showView("home")));
document.querySelectorAll("[data-nav='book']").forEach((a) =>
  a.addEventListener("click", (e) => {
    e.preventDefault();
    showView("home");
    setTimeout(() => $("discover").scrollIntoView({ behavior: "smooth" }), 50);
  }));

/* Live open-now pill across all venues (real posted hours). */
async function refreshOpenPill() {
  try {
    const data = await getRestaurants("", 0, 50);
    const list = data.content || [];
    if (!list.length) return;
    const open = list.filter((r) => isOpenNow(r)).length;
    const pill = $("open-pill");
    pill.textContent = open ? `${open} of ${list.length} open now` : "Opens at noon";
    pill.classList.toggle("closed", !open);
  } catch { $("open-pill").textContent = "Cairo · live"; }
}

/* ── hero searchbar: area + date + party + query ── */
function initSearchbar() {
  const d = $("sb-date");
  d.value = new Date(Date.now() + 864e5).toISOString().slice(0, 10);
  d.min = new Date().toISOString().slice(0, 10);
  $("sb-party").innerHTML = Array.from({ length: 10 }, (_, i) =>
    `<option ${i === 1 ? "selected" : ""} value="${i + 1}">${i + 1} ${i ? "guests" : "guest"}</option>`).join("");
  $("searchbar").addEventListener("submit", (e) => {
    e.preventDefault();
    state.area = $("sb-area").value.trim();
    state.q = $("sb-q").value.trim();
    state.page = 0;
    state.cuisine = "All"; state.price = ""; state.openOnly = false; state.topOnly = false;
    bookingCtx.date = $("sb-date").value;
    bookingCtx.party = +$("sb-party").value || 1;
    loadGrid().then(() => $("discover").scrollIntoView({ behavior: "smooth" }));
  });
}

/* ── collections ── */
const COLLECTIONS = [
  { name: "Italian Night", sub: "Pasta, pizza, Chianti mood", q: "italia", img: "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=600&q=80&auto=format&fit=crop" },
  { name: "Sushi & Asian", sub: "Fresh rolls, quiet counter", q: "sushi", img: "https://images.unsplash.com/photo-1553621042-f6e147245754?w=600&q=80&auto=format&fit=crop" },
  { name: "Egyptian Classics", sub: "Grills with a Nile view", q: "saraya", img: "https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=600&q=80&auto=format&fit=crop" },
  { name: "Maadi Evenings", sub: "Leafy streets, cozy rooms", q: "maadi", img: "https://images.unsplash.com/photo-1552566626-52f8b828add9?w=600&q=80&auto=format&fit=crop" },
  { name: "Zayed Table", sub: "West-side favourite", q: "zayed", img: "https://images.unsplash.com/photo-1467003909585-2f8a72700288?w=600&q=80&auto=format&fit=crop" },
];
function renderCollections() {
  $("collections-row").innerHTML = COLLECTIONS.map((c, i) => `
    <button class="collection" data-i="${i}">
      <img loading="lazy" src="${c.img}" alt="${esc(c.name)}" ${imgFallback()} />
      <div><b>${esc(c.name)}</b><small>${esc(c.sub)}</small></div>
    </button>`).join("");
  document.querySelectorAll(".collection").forEach((el) => {
    reveal(el);
    el.addEventListener("click", () => {
      const c = COLLECTIONS[+el.dataset.i];
      $("sb-q").value = c.q;
      $("sb-area").value = "";
      state.q = c.q; state.area = ""; state.page = 0;
      state.cuisine = "All"; state.price = ""; state.openOnly = false; state.topOnly = false;
      loadGrid().then(() => $("discover").scrollIntoView({ behavior: "smooth" }));
    });
  });
  /* real spot counts — one cheap totalElements lookup each */
  document.querySelectorAll(".collection").forEach(async (el) => {
    try {
      const data = await getRestaurants(COLLECTIONS[+el.dataset.i].q, 0, 1);
      const n = data.totalElements ?? 0;
      const small = el.querySelector("small");
      if (small) small.textContent = `${COLLECTIONS[+el.dataset.i].sub} · ${n} spot${n === 1 ? "" : "s"}`;
    } catch { /* keep static sub */ }
  });
}

/* scrollspy: the nav always knows where you are */
function initScrollspy() {
  const map = [["#discover", "Restaurants"], ["#collections", "Collections"], ["#how", "How booking works"]];
  const links = [...document.querySelectorAll(".nav-links a")];
  const io = new IntersectionObserver((entries) => {
    entries.forEach((e) => {
      if (!e.isIntersecting) return;
      const label = (map.find(([sel]) => e.target.matches(sel)) || [])[1];
      links.forEach((a) => a.classList.toggle("active", a.textContent.trim() === label));
    });
  }, { rootMargin: "-40% 0px -55% 0px" });
  map.forEach(([sel]) => { const s = document.querySelector(sel); if (s) io.observe(s); });
}

/* ── restaurant grid ── */
function renderCuisineChips(cuisines) {
  const all = ["All", ...cuisines];
  if (!all.includes(state.cuisine)) state.cuisine = "All";
  $("cuisine-chips").innerHTML = all.map((c) =>
    `<button class="chip ${state.cuisine === c ? "active" : ""}" data-c="${esc(c)}">${esc(c)}</button>`).join("");
  document.querySelectorAll("#cuisine-chips .chip").forEach((el) =>
    el.addEventListener("click", () => { state.cuisine = el.dataset.c; wireFilterChips(); renderGrid(); }));
}
function wireFilterChips() {
  document.querySelectorAll("#price-chips .chip").forEach((el) =>
    el.classList.toggle("active", el.dataset.p === state.price));
  const fo = $("f-open"), ft = $("f-top");
  if (fo) fo.setAttribute("aria-pressed", String(state.openOnly));
  if (ft) ft.setAttribute("aria-pressed", String(state.topOnly));
}
function initFilters() {
  document.querySelectorAll("#price-chips .chip").forEach((el) =>
    el.addEventListener("click", () => { state.price = el.dataset.p; wireFilterChips(); renderGrid(); }));
  $("f-open").addEventListener("click", () => {
    state.openOnly = !state.openOnly; wireFilterChips(); renderGrid();
  });
  $("f-top").addEventListener("click", () => {
    state.topOnly = !state.topOnly; wireFilterChips(); renderGrid();
  });
  wireFilterChips();
}
function boardTitle() {
  const terms = [state.area, state.q].filter(Boolean);
  const when = bookingCtx.date
    ? (bookingCtx.date === todayStr() ? "tonight" : `for ${bookingCtx.date}`)
    : "tonight";
  if (!terms.length) return `Available ${when} in Cairo`;
  return `Available ${when}: ${terms.join(" · ")}`;
}
function whenLabel() {
  return bookingCtx.date === todayStr() ? "tonight" : `on ${bookingCtx.date}`;
}
function isPastSlot(mins) {
  const dt = new Date(`${bookingCtx.date}T${slotVal(mins)}:00`);
  return dt.getTime() < Date.now() - 5 * 60e3;
}
async function loadGrid() {
  const grid = $("grid");
  grid.innerHTML = "<div class='skeleton'></div><div class='skeleton'></div><div class='skeleton'></div>";
  try {
    const q = [state.area, state.q].filter(Boolean).join(" ");
    const data = await getRestaurants(q, state.page, state.size);
    state.totalPages = data.totalPages || 1;
    state.cache.clear();
    (data.content || []).forEach((r) => state.cache.set(r.id, r));
    $("results-title").textContent = boardTitle();
    $("results-sub").textContent = `${data.totalElements ?? 0} venues · hours and reviews verified live`;
    $("pageinfo").textContent = `Page ${(data.page ?? 0) + 1} of ${state.totalPages}`;
    $("prev").disabled = state.page <= 0;
    $("next").disabled = state.page >= state.totalPages - 1;
    renderCuisineChips([...new Set((data.content || []).map((r) => r.cuisine).filter(Boolean))]);
    wireFilterChips();
    await renderGrid();
  } catch {
    grid.innerHTML = `<div class="empty"><h3>The book is unreachable.</h3><p>Start the backend, then refresh. Swagger: <a href="/swagger-ui/index.html">/swagger-ui.html</a></p></div>`;
  }
}
async function renderGrid() {
  const grid = $("grid");
  let list = [...state.cache.values()];
  if (state.cuisine !== "All") list = list.filter((r) => r.cuisine === state.cuisine);
  if (state.price) list = list.filter((r) => r.priceRange === state.price);
  if (state.openOnly) list = list.filter((r) => isOpenNow(r));
  const ratings = await Promise.all(list.map((r) => getRating(r.id)));
  let items = list.map((r, i) => ({ r, rating: ratings[i] }));
  if (state.topOnly) items = items.filter((x) => x.rating.avg >= 4.5);
  if (!items.length) {
    grid.innerHTML = `<div class="empty"><h3>Nothing on the board for that.</h3><p>Loosen a filter or clear the search.</p></div>`;
    return;
  }
  grid.innerHTML = "";
  const topId = (() => {
    let best = null;
    items.forEach((x) => {
      if (x.rating.count > 0 && (!best || x.rating.avg > best.rating.avg)) best = x;
    });
    return items.length > 1 && best ? best.r.id : null;
  })();
  items.forEach((x, i) => {
    const el = card(x.r, x.rating, i === 0 && items.length > 1 && !state.q && !state.area, x.r.id === topId);
    reveal(el);
    grid.appendChild(el);
  });
}
function metaLine(r) {
  const open = isOpenNow(r);
  return { open };
}
function card(r, rating, featured, isTop) {
  const el = document.createElement("article");
  el.className = "card" + (featured ? " featured" : "");
  const slots = venueSlots(r);
  const open = isOpenNow(r);
  const ar = AR_NAMES[r.name] ? `<span class="ar">${esc(AR_NAMES[r.name])}</span>` : "";
  const revLabel = rating.count
    ? `${stars(rating.avg)} <b>${rating.avg.toFixed(1)}</b> <span>(${rating.count} verified)</span>`
    : `${stars(0)} <span>New here — your review could be first</span>`;
  const desc = featured ? `<p class="venue-desc">${esc(r.description || "")}</p>` : "";
  const slotBtns = slots.map((s) => {
    const past = isPastSlot(s);
    return `<button class="slot" data-slot="${s}" ${past ? "disabled title='This hour has passed'" : ""}>${fmtSlot(s)}</button>`;
  }).join("");
  const cta = featured
    ? `<div class="card-cta"><button class="btn btn-red btn-sm" data-act="reserve">Reserve a table →</button></div>`
    : "";
  el.innerHTML = `
    <div class="card-img">
      <img loading="lazy" src="${esc(r.imageUrl || FALLBACK_IMG)}" alt="${esc(r.name)} dining room" ${imgFallback()} />
      ${isTop ? `<span class="ribbon">Top rated</span>` : ""}
      ${r.priceRange ? `<span class="price-badge">${esc(r.priceRange)}</span>` : ""}
    </div>
    <div class="card-body">
      <h3>${esc(r.name)} ${ar}</h3>
      <div class="rating-row">${revLabel}</div>
      <p class="meta">${ICON.pin} ${esc([r.cuisine, r.priceRange, areaOf(r)].filter(Boolean).join("  ·  "))} <span class="dot-sep">·</span> ${open ? "Open now" : `Opens ${esc((r.openingTime || "12:00").slice(0, 5))}`}</p>
      ${desc}
      <p class="avail-label">Available ${esc(whenLabel())}</p>
      <div class="slots">${slotBtns}</div>
      ${rating.count ? `<p class="booked-note">${ICON.check.replace("<svg", "<svg width='13' height='13'")} ${rating.avg >= 4.5 ? "A house favourite" : "Reviewed"} by ${rating.count} verified diner${rating.count > 1 ? "s" : ""}</p>` : ""}
      ${cta}
    </div>`;
  el.addEventListener("click", (e) => {
    if (e.target.closest("[disabled]")) return;
    const slotBtn = e.target.closest(".slot");
    openVenue(r.id, slotBtn ? +slotBtn.dataset.slot : null);
  });
  return el;
}
$("prev").onclick = () => { if (state.page > 0) { state.page--; loadGrid(); } };
$("next").onclick = () => { if (state.page < state.totalPages - 1) { state.page++; loadGrid(); } };

/* ── views ── */
function showView(which) {
  $("view-home").classList.toggle("hidden", which !== "home");
  $("view-venue").classList.toggle("hidden", which !== "venue");
  $("mobile-book").classList.toggle("hidden", which !== "venue");
  document.querySelectorAll('script[data-venue-ld]').forEach((s) => s.remove());
  window.scrollTo({ top: 0 });
}
$("venue-back").onclick = () => showView("home");

/* ── venue page ── */
async function openVenue(id, preselectSlot) {
  showView("venue");
  const body = $("venue-body");
  body.innerHTML = "<div class='skeleton' style='height:480px'></div>";
  try {
    const [r, menu, revs] = await Promise.all([
      api(`/api/restaurants/${id}`),
      getMenu(id),
      api(`/api/restaurants/${id}/reviews?page=0&size=20`).catch(() => ({ content: [] })),
    ]);
    const rating = await getRating(id);
    const dishes = menu.flatMap((c) => c.menuItems || []);
    const gallery = [r.imageUrl || FALLBACK_IMG, dishes[0]?.imageUrl, dishes[1]?.imageUrl].filter(Boolean);
    while (gallery.length < 3) gallery.push(FALLBACK_IMG);
    const open = isOpenNow(r);
    const ar = AR_NAMES[r.name] ? `<span class="ar">${esc(AR_NAMES[r.name])}</span>` : "";

    const revList = revs.content || [];
    const revHtml = revList.length ? revList.map((v) => `
      <div class="review">
        <span class="avatar" style="background:${avatarColor(v.userName || "Guest")}">${esc(initials(v.userName || "Guest"))}</span>
        <div><div class="who">${stars(v.rating)} <b>${esc(v.userName || "Guest")}</b>
          <time>${v.createdAt ? new Date(v.createdAt).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" }) : ""}</time></div>
          <p>${esc(v.comment || "")}</p></div>
      </div>`).join("")
      : `<p class="muted">No ink on this page yet. Book a completed visit and your words go here, dated and signed.</p>`;

    const pills = menu.map((c, i) => {
      const n = (c.menuItems || []).length;
      return `<a href="#menu-${i}">${esc(c.name)} · ${n}</a>`;
    }).join("");
    const menuHtml = menu.length ? menu.map((c, i) => `
      <div class="menu-cat" id="menu-${i}"><h3>${esc(c.name)} <span class="muted" style="font-family:Inter;font-size:.85rem;font-weight:400">· ${(c.menuItems || []).length} dishes</span></h3>
        ${(c.menuItems || []).map((m) => `
          <div class="dish ${m.available ? "" : "unavail"}">
            <img loading="lazy" src="${esc(m.imageUrl || DISH_FALLBACK)}" alt="${esc(m.name)}" ${imgFallback(DISH_FALLBACK)} />
            <div class="dish-info"><b>${esc(m.name)}${m.available ? "" : " · unavailable"}</b><small>${esc(m.description || "")}</small></div>
            <span class="price">${m.price} EGP</span>
          </div>`).join("")}
      </div>`).join("")
      : `<p class="muted">The kitchen has not pinned its menu yet.</p>`;

    const daySlots = allDaySlots(r);
    const bkDate = bookingCtx.date || new Date(Date.now() + 864e5).toISOString().slice(0, 10);
    const bkParty = bookingCtx.party || 1;
    const defaultSlot = preselectSlot ?? daySlots.find((s) => s >= 19 * 60) ?? daySlots[0];
    const mapsUrl = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(r.name + " " + (r.location || "Cairo"))}`;

    body.innerHTML = `
      <div class="venue-head">
        <h1>${esc(r.name)} ${ar}</h1>
        <div class="rating-row">
          ${rating.count ? `${stars(rating.avg)} <b>${rating.avg.toFixed(1)}</b> <span>(${rating.count} verified reviews)</span>` : `<span>Not yet rated — first review goes on the wall</span>`}
          <span class="open-badge ${open ? "" : "closed"}">${open ? `Open now · closes ${fmtHM(toMin(r.closingTime, 23 * 60 + 59))}` : `Closed · opens ${fmtHM(toMin(r.openingTime, 12 * 60))}`}</span>
        </div>
        <p class="meta">${ICON.pin} ${esc([r.cuisine, r.priceRange, r.location].filter(Boolean).join("  ·  "))}</p>
      </div>
      <div class="gallery">
        <img class="main" src="${esc(gallery[0])}" alt="${esc(r.name)} dining room" ${imgFallback()} />
        <div class="side"><img src="${esc(gallery[1])}" alt="Signature plate" ${imgFallback()} /><img src="${esc(gallery[2])}" alt="From the kitchen" ${imgFallback()} /></div>
      </div>
      ${menu.length > 1 ? `<nav class="menu-pills" aria-label="Menu sections">${pills}</nav>` : ""}
      <div class="venue-cols">
        <div class="venue-main">
          <h2>The room</h2>
          <p>${esc(r.description || "A hand-picked Sofra partner. Menus, hours and availability are kept live by the restaurant team.")}</p>
          <div class="policy">
            <div>${ICON.check}<span>Free cancellation<small>up to 3h before</small></span></div>
            <div>${ICON.book}<span>Pay at the table<small>no card to book</small></span></div>
            <div>${ICON.users}<span>Parties to 10<small>bigger? call ahead</small></span></div>
          </div>
          <h2>Menu</h2>
          ${menuHtml}
          <h2>The guest book ${rating.count ? `(${rating.count})` : ""}</h2>
          ${rating.count ? `
          <div class="rev-summary">
            <span class="rev-score">${rating.avg.toFixed(1)}</span>
            <div>${stars(rating.avg)}
              <small>${rating.count} verified review${rating.count > 1 ? "s" : ""}</small>
              <span class="verified-note">${ICON.check} Written only after completed visits</span>
            </div>
          </div>` : ""}
          ${revHtml}
        </div>
        <aside>
          <div class="book-card" id="book-card">
            <h3><span class="live-dot" aria-hidden="true"></span>Book this table</h3>
            <p class="muted">Live availability · confirmed by the house</p>
            <div class="book-row">
              <label class="field full"><span>Date</span><input type="date" id="bk-date" value="${bkDate}" min="${new Date().toISOString().slice(0, 10)}" /></label>
              <label class="field"><span>Time</span><select id="bk-time">${daySlots.map((s) => `<option ${s === defaultSlot ? "selected" : ""} value="${s}">${fmtSlot(s)}</option>`).join("")}</select></label>
              <label class="field"><span>Party</span><select id="bk-party">${Array.from({ length: 10 }, (_, i) => `<option ${i + 1 === bkParty ? "selected" : ""} value="${i + 1}">${i + 1}</option>`).join("")}</select></label>
            </div>
            <button class="btn btn-red btn-block" id="bk-check">Check availability</button>
            <div class="tables" id="bk-tables"></div>
            <button class="btn btn-block hidden" id="bk-reserve" style="border-color:var(--brand);color:var(--brand)">Reserve this table</button>
            <p class="fine">Confirmation needs a free account and a JWT — the button hands you the exact request.</p>
          </div>
          <div class="info-card">
            <div><b>Hours</b>${ICON.clock}<span>${esc((r.openingTime || "?").slice(0, 5))} – ${esc((r.closingTime || "?").slice(0, 5))}</span></div>
            <div><b>Phone</b>${ICON.phone}<a href="tel:${esc((r.phone || "").replace(/\s/g, ""))}">${esc(r.phone || "—")}</a></div>
            <div><b>Area</b>${ICON.pin}<span>${esc(r.location || "—")}</span></div>
            <div><b>Find</b>${ICON.nav}<a href="${mapsUrl}" target="_blank" rel="noopener">Directions on Maps</a></div>
          </div>
        </aside>
      </div>`;

    /* structured data: the machine-readable version of this page */
    const ld = document.createElement("script");
    ld.type = "application/ld+json";
    ld.setAttribute("data-venue-ld", "1");
    ld.textContent = JSON.stringify({
      "@context": "https://schema.org",
      "@type": "Restaurant",
      name: r.name,
      servesCuisine: r.cuisine,
      priceRange: r.priceRange,
      telephone: r.phone,
      address: r.location,
      openingHours: `${(r.openingTime || "12:00").slice(0, 5)}-${(r.closingTime || "23:00").slice(0, 5)}`,
      ...(rating.count ? { aggregateRating: { "@type": "AggregateRating", ratingValue: rating.avg.toFixed(1), reviewCount: rating.count } } : {}),
    });
    document.head.appendChild(ld);

    /* sticky mobile bar */
    $("mb-name").textContent = r.name;
    $("mb-meta").textContent = `${r.cuisine || ""} · ${areaOf(r)}`;
    $("mb-go").onclick = () => $("book-card").scrollIntoView({ behavior: "smooth", block: "center" });

    let chosen = null;
    $("bk-check").onclick = async () => {
      const box = $("bk-tables");
      box.innerHTML = `<p class="muted">Asking the house…</p>`;
      $("bk-reserve").classList.add("hidden");
      chosen = null;
      try {
        const date = $("bk-date").value, slot = +$("bk-time").value, party = +$("bk-party").value;
        const start = new Date(`${date}T${slotVal(slot)}:00`).toISOString();
        const end = new Date(new Date(start).getTime() + 2 * 3600e3).toISOString();
        const tables = await api(`/api/restaurants/${id}/available-tables?startTime=${encodeURIComponent(start)}&endTime=${encodeURIComponent(end)}`);
        const fitting = tables.filter((t) => t.capacity >= party);
        if (!fitting.length) {
          box.innerHTML = `<div class="avail-summary none">Full house for ${party} at ${fmtSlot(slot)}.</div>
            <button class="btn btn-ghost btn-sm btn-block" id="bk-tmr" style="margin-top:8px">View tomorrow instead</button>`;
          $("bk-tmr").onclick = () => {
            const d = new Date(`${date}T12:00:00`);
            $("bk-date").value = new Date(d.getTime() + 864e5).toISOString().slice(0, 10);
            $("bk-check").click();
          };
          return;
        }
        const low = fitting.length <= 2;
        box.innerHTML = `<div class="avail-summary ${low ? "low" : ""}">${fitting.length} table${fitting.length > 1 ? "s" : ""} left for ${party} · ${fmtSlot(slot)}${low ? " — going fast" : ""}</div>` + fitting.map((t) => `
          <div class="table-opt" data-id="${t.id}" role="button" tabindex="0">
            <span>Table ${t.tableNumber}</span><span class="cap">seats ${t.capacity}</span>
          </div>`).join("");
        const pick = (el) => {
          box.querySelectorAll(".table-opt").forEach((x) => x.classList.remove("selected"));
          el.classList.add("selected");
          chosen = +el.dataset.id;
          const btn = $("bk-reserve");
          btn.classList.remove("hidden");
          btn.onclick = () => openCmd(chosen, date, slot, party);
        };
        box.querySelectorAll(".table-opt").forEach((el) => {
          el.addEventListener("click", () => pick(el));
          el.addEventListener("keydown", (e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); pick(el); } });
        });
        toast(`${fitting.length} free table${fitting.length > 1 ? "s" : ""} at ${fmtSlot(slot)} — pick one.`);
      } catch { box.innerHTML = `<p class="muted">The house line is down — check the backend is running.</p>`; }
    };
    if (preselectSlot != null) $("bk-check").click();
  } catch {
    body.innerHTML = `<div class="empty"><h3>This page left the building.</h3><p><a href="#" id="retry-home">Back to the board</a></p></div>`;
    $("retry-home").onclick = (e) => { e.preventDefault(); showView("home"); };
  }
}

/* ── boot ── */
initSearchbar();
initFilters();
initScrollspy();
renderCollections();
refreshOpenPill();
loadGrid();
