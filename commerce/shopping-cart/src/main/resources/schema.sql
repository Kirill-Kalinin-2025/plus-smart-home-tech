-- создаём таблицу carts
CREATE TABLE IF NOT EXISTS carts (
    cart_id UUID PRIMARY KEY,
    username VARCHAR NOT NULL UNIQUE,
    state VARCHAR NOT NULL
);

-- создаём таблицу cart_products
CREATE TABLE IF NOT EXISTS cart_products (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cart_id UUID REFERENCES carts(cart_id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL
);