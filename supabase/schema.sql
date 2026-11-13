-- Omid Store database schema
-- Apply only after the Supabase project has been created.
-- Public clients must use the publishable key; never ship service_role/secret keys.

create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null default '',
  phone text not null default '',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.categories (
  id uuid primary key default gen_random_uuid(),
  name text not null unique,
  sort_order integer not null default 0,
  is_active boolean not null default true,
  created_at timestamptz not null default now()
);

create table if not exists public.products (
  id uuid primary key default gen_random_uuid(),
  category_id uuid references public.categories(id) on delete set null,
  name text not null,
  description text not null default '',
  price_toman bigint not null check (price_toman >= 0),
  image_url text,
  stock integer not null default 0 check (stock >= 0),
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.orders (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete restrict,
  customer_name text not null,
  phone text not null,
  delivery_address text not null,
  status text not null default 'pending'
    check (status in ('pending','confirmed','preparing','out_for_delivery','delivered','cancelled')),
  payment_method text not null default 'cash_on_delivery'
    check (payment_method = 'cash_on_delivery'),
  total_toman bigint not null check (total_toman >= 0),
  notes text not null default '',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.order_items (
  id uuid primary key default gen_random_uuid(),
  order_id uuid not null references public.orders(id) on delete cascade,
  product_id uuid references public.products(id) on delete set null,
  product_name text not null,
  unit_price_toman bigint not null check (unit_price_toman >= 0),
  quantity integer not null check (quantity > 0),
  line_total_toman bigint not null check (line_total_toman >= 0)
);

create index if not exists products_active_idx on public.products(is_active, created_at desc);
create index if not exists orders_user_created_idx on public.orders(user_id, created_at desc);
create index if not exists orders_status_created_idx on public.orders(status, created_at desc);
create index if not exists order_items_order_idx on public.order_items(order_id);

-- Admin authority comes only from server-managed app_metadata, never user-editable user_metadata.
create or replace function public.is_store_admin()
returns boolean
language sql
stable
security invoker
set search_path = ''
as $$
  select coalesce((auth.jwt() -> 'app_metadata' ->> 'role') = 'admin', false);
$$;

alter table public.profiles enable row level security;
alter table public.categories enable row level security;
alter table public.products enable row level security;
alter table public.orders enable row level security;
alter table public.order_items enable row level security;

-- Profiles: customers manage only their own profile; admins can inspect customer profiles.
drop policy if exists "profile read own or admin" on public.profiles;
create policy "profile read own or admin" on public.profiles
for select to authenticated
using ((select auth.uid()) = id or (select public.is_store_admin()));

drop policy if exists "profile insert own" on public.profiles;
create policy "profile insert own" on public.profiles
for insert to authenticated
with check ((select auth.uid()) = id);

drop policy if exists "profile update own" on public.profiles;
create policy "profile update own" on public.profiles
for update to authenticated
using ((select auth.uid()) = id)
with check ((select auth.uid()) = id);

-- Categories and products are readable when active. Only admins can mutate them.
drop policy if exists "active categories are public" on public.categories;
create policy "active categories are public" on public.categories
for select to anon, authenticated
using (is_active or (select public.is_store_admin()));

drop policy if exists "admins manage categories" on public.categories;
create policy "admins manage categories" on public.categories
for all to authenticated
using ((select public.is_store_admin()))
with check ((select public.is_store_admin()));

drop policy if exists "active products are public" on public.products;
create policy "active products are public" on public.products
for select to anon, authenticated
using (is_active or (select public.is_store_admin()));

drop policy if exists "admins manage products" on public.products;
create policy "admins manage products" on public.products
for all to authenticated
using ((select public.is_store_admin()))
with check ((select public.is_store_admin()));

-- Orders: customers create and read only their own orders; admins manage all orders.
drop policy if exists "customers read own orders or admin" on public.orders;
create policy "customers read own orders or admin" on public.orders
for select to authenticated
using ((select auth.uid()) = user_id or (select public.is_store_admin()));

drop policy if exists "customers create own orders" on public.orders;
create policy "customers create own orders" on public.orders
for insert to authenticated
with check (
  (select auth.uid()) = user_id
  and status = 'pending'
  and payment_method = 'cash_on_delivery'
);

drop policy if exists "admins update orders" on public.orders;
create policy "admins update orders" on public.orders
for update to authenticated
using ((select public.is_store_admin()))
with check ((select public.is_store_admin()));

drop policy if exists "order items visible to owner or admin" on public.order_items;
create policy "order items visible to owner or admin" on public.order_items
for select to authenticated
using (
  exists (
    select 1 from public.orders o
    where o.id = order_id
      and (o.user_id = (select auth.uid()) or (select public.is_store_admin()))
  )
);

drop policy if exists "customers add items to own pending orders" on public.order_items;
create policy "customers add items to own pending orders" on public.order_items
for insert to authenticated
with check (
  exists (
    select 1 from public.orders o
    where o.id = order_id
      and o.user_id = (select auth.uid())
      and o.status = 'pending'
  )
);

drop policy if exists "admins manage order items" on public.order_items;
create policy "admins manage order items" on public.order_items
for all to authenticated
using ((select public.is_store_admin()))
with check ((select public.is_store_admin()));

-- Explicitly expose only the intended tables to the Data API (new Supabase projects
-- may not expose public-schema tables automatically).
grant select on public.categories, public.products to anon, authenticated;
grant select, insert, update on public.profiles to authenticated;
grant select, insert on public.orders to authenticated;
grant update on public.orders to authenticated;
grant select, insert on public.order_items to authenticated;
grant update, delete on public.categories, public.products to authenticated;
grant all on public.categories, public.products, public.orders, public.order_items to authenticated;

