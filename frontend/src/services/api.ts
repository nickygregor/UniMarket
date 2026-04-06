// src/services/api.ts
const BASE_URL = "http://localhost:8080";

function authHeader(): HeadersInit {
  const token = localStorage.getItem("token");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { "Content-Type": "application/json", ...authHeader(), ...options.headers },
    ...options,
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || "Request failed");
  return data as T;
}

// ── Types ────────────────────────────────────────────────────────────────────

export interface User {
  id: number; firstName: string; lastName: string;
  email: string; phoneNumber: string; userId: string;
  role: string; isActive: boolean;
}

export interface AuthResponse { token: string; user: User; }

export interface Listing {
  id: number; sellerId: number; sellerName: string;
  title: string; description: string; price: number;
  category: string; imageUrl: string | null;
  isActive: boolean; createdAt: number;
}

export interface CartItem {
  id: number; listing: Listing; quantity: number; subtotal: number;
}
export interface Cart { items: CartItem[]; totalAmount: number; }

export interface OrderItem {
  listingId: number; title: string; quantity: number;
  priceAtPurchase: number; subtotal: number;
}
export interface Order {
  id: number; buyerId: number; items: OrderItem[];
  totalAmount: number; status: string; createdAt: number;
}

// ── Auth ──────────────────────────────────────────────────────────────────────

export const authApi = {
  login:    (userId: string, password: string) =>
    request<AuthResponse>("/auth/login", { method: "POST", body: JSON.stringify({ userId, password }) }),
  register: (data: object) =>
    request<AuthResponse>("/auth/register", { method: "POST", body: JSON.stringify(data) }),
};

// ── Listings ──────────────────────────────────────────────────────────────────

export const listingApi = {
  getAll:  (keyword?: string, category?: string) => {
    const params = new URLSearchParams();
    if (keyword)  params.set("keyword",  keyword);
    if (category) params.set("category", category);
    return request<Listing[]>(`/listings?${params}`);
  },
  getById:          (id: number)    => request<Listing>(`/listings/${id}`),
  getMyListings:    ()              => request<Listing[]>("/seller/listings"),
  createListing:    (data: object)  => request<Listing>("/seller/listings", { method: "POST", body: JSON.stringify(data) }),
  updateListing:    (id: number, data: object) => request<Listing>(`/seller/listings/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  deleteListing:    (id: number)    => request<{ message: string }>(`/seller/listings/${id}`, { method: "DELETE" }),
};

// ── Cart ──────────────────────────────────────────────────────────────────────

export const cartApi = {
  getCart:      ()                           => request<Cart>("/buyer/cart"),
  addItem:      (listingId: number, qty = 1) => request<{ message: string }>("/buyer/cart/add", { method: "POST", body: JSON.stringify({ listingId, quantity: qty }) }),
  removeItem:   (cartItemId: number)         => request<{ message: string }>(`/buyer/cart/remove/${cartItemId}`, { method: "DELETE" }),
};

// ── Orders ────────────────────────────────────────────────────────────────────

export const orderApi = {
  checkout:   ()          => request<Order>("/buyer/orders/checkout", { method: "POST" }),
  getOrders:  ()          => request<Order[]>("/buyer/orders"),
  getById:    (id: number)=> request<Order>(`/buyer/orders/${id}`),
};

// ── Admin ─────────────────────────────────────────────────────────────────────

export const adminApi = {
  getAllUsers:     ()          => request<User[]>("/admin/users"),
  activateUser:   (id: number)=> request<{ message: string }>(`/admin/users/${id}/activate`,   { method: "PUT" }),
  deactivateUser: (id: number)=> request<{ message: string }>(`/admin/users/${id}/deactivate`, { method: "PUT" }),
  deleteListing:  (id: number)=> request<{ message: string }>(`/admin/listings/${id}`,          { method: "DELETE" }),
};
