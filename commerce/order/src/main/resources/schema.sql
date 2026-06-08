CREATE TABLE IF NOT EXISTS orders (
    order_id UUID PRIMARY KEY,
    username VARCHAR NOT NULL,
    shopping_cart_id UUID,
    payment_id UUID,
    delivery_id UUID,
    state VARCHAR NOT NULL,
    delivery_weight DOUBLE PRECISION,
    delivery_volume DOUBLE PRECISION,
    fragile BOOLEAN,
    total_price DOUBLE PRECISION,
    delivery_price DOUBLE PRECISION,
    product_price DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS order_products (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id UUID REFERENCES orders(order_id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL
);