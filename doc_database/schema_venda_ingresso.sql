--
-- PostgreSQL database dump
--

\restrict 7K2noMQOfxHMmbRbbMAXNoqdP01uZqxeloDXlULThfFZ8ln9eyvoDlzA2905S5V

-- Dumped from database version 18.3
-- Dumped by pg_dump version 18.4

-- Started on 2026-06-07 17:31:08

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 6 (class 2615 OID 16591)
-- Name: auth; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA auth;


ALTER SCHEMA auth OWNER TO postgres;

--
-- TOC entry 7 (class 2615 OID 16614)
-- Name: eventos; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA eventos;


ALTER SCHEMA eventos OWNER TO postgres;

--
-- TOC entry 8 (class 2615 OID 16668)
-- Name: vendas; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA vendas;


ALTER SCHEMA vendas OWNER TO postgres;

--
-- TOC entry 870 (class 1247 OID 16593)
-- Name: role_usuario; Type: TYPE; Schema: auth; Owner: postgres
--

CREATE TYPE auth.role_usuario AS ENUM (
    'ADMIN',
    'CLIENTE'
);


ALTER TYPE auth.role_usuario OWNER TO postgres;

--
-- TOC entry 876 (class 1247 OID 16616)
-- Name: status_evento; Type: TYPE; Schema: eventos; Owner: postgres
--

CREATE TYPE eventos.status_evento AS ENUM (
    'DISPONIVEL',
    'ESGOTADO'
);


ALTER TYPE eventos.status_evento OWNER TO postgres;

--
-- TOC entry 882 (class 1247 OID 16635)
-- Name: status_ingresso_tipo; Type: TYPE; Schema: eventos; Owner: postgres
--

CREATE TYPE eventos.status_ingresso_tipo AS ENUM (
    'DISPONIVEL',
    'ESGOTADO'
);


ALTER TYPE eventos.status_ingresso_tipo OWNER TO postgres;

--
-- TOC entry 900 (class 1247 OID 16720)
-- Name: status_ingresso; Type: TYPE; Schema: vendas; Owner: postgres
--

CREATE TYPE vendas.status_ingresso AS ENUM (
    'ATIVO',
    'USADO',
    'CANCELADO'
);


ALTER TYPE vendas.status_ingresso OWNER TO postgres;

--
-- TOC entry 906 (class 1247 OID 16744)
-- Name: status_pagamento; Type: TYPE; Schema: vendas; Owner: postgres
--

CREATE TYPE vendas.status_pagamento AS ENUM (
    'PAGO',
    'PENDENTE',
    'RECUSADO'
);


ALTER TYPE vendas.status_pagamento OWNER TO postgres;

--
-- TOC entry 891 (class 1247 OID 16670)
-- Name: status_pedido; Type: TYPE; Schema: vendas; Owner: postgres
--

CREATE TYPE vendas.status_pedido AS ENUM (
    'PENDENTE',
    'PAGO',
    'CANCELADO',
    'EXPIRADO'
);


ALTER TYPE vendas.status_pedido OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 223 (class 1259 OID 16598)
-- Name: usuarios; Type: TABLE; Schema: auth; Owner: postgres
--

CREATE TABLE auth.usuarios (
    id bigint NOT NULL,
    nome character varying(200) NOT NULL,
    email character varying(255) NOT NULL,
    senha character varying(255) NOT NULL,
    role auth.role_usuario DEFAULT 'CLIENTE'::auth.role_usuario NOT NULL
);


ALTER TABLE auth.usuarios OWNER TO postgres;

--
-- TOC entry 222 (class 1259 OID 16597)
-- Name: usuarios_id_seq; Type: SEQUENCE; Schema: auth; Owner: postgres
--

CREATE SEQUENCE auth.usuarios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE auth.usuarios_id_seq OWNER TO postgres;

--
-- TOC entry 4411 (class 0 OID 0)
-- Dependencies: 222
-- Name: usuarios_id_seq; Type: SEQUENCE OWNED BY; Schema: auth; Owner: postgres
--

ALTER SEQUENCE auth.usuarios_id_seq OWNED BY auth.usuarios.id;


--
-- TOC entry 229 (class 1259 OID 16659)
-- Name: checkin; Type: TABLE; Schema: eventos; Owner: postgres
--

CREATE TABLE eventos.checkin (
    id bigint NOT NULL,
    ingresso_id bigint NOT NULL,
    realizado_em timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE eventos.checkin OWNER TO postgres;

--
-- TOC entry 228 (class 1259 OID 16658)
-- Name: checkin_id_seq; Type: SEQUENCE; Schema: eventos; Owner: postgres
--

CREATE SEQUENCE eventos.checkin_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE eventos.checkin_id_seq OWNER TO postgres;

--
-- TOC entry 4412 (class 0 OID 0)
-- Dependencies: 228
-- Name: checkin_id_seq; Type: SEQUENCE OWNED BY; Schema: eventos; Owner: postgres
--

ALTER SEQUENCE eventos.checkin_id_seq OWNED BY eventos.checkin.id;


--
-- TOC entry 225 (class 1259 OID 16622)
-- Name: eventos; Type: TABLE; Schema: eventos; Owner: postgres
--

CREATE TABLE eventos.eventos (
    id bigint NOT NULL,
    nome character varying(200) NOT NULL,
    descricao text,
    local character varying(255),
    data_evento timestamp without time zone NOT NULL,
    status eventos.status_evento DEFAULT 'DISPONIVEL'::eventos.status_evento
);


ALTER TABLE eventos.eventos OWNER TO postgres;

--
-- TOC entry 224 (class 1259 OID 16621)
-- Name: eventos_id_seq; Type: SEQUENCE; Schema: eventos; Owner: postgres
--

CREATE SEQUENCE eventos.eventos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE eventos.eventos_id_seq OWNER TO postgres;

--
-- TOC entry 4413 (class 0 OID 0)
-- Dependencies: 224
-- Name: eventos_id_seq; Type: SEQUENCE OWNED BY; Schema: eventos; Owner: postgres
--

ALTER SEQUENCE eventos.eventos_id_seq OWNED BY eventos.eventos.id;


--
-- TOC entry 227 (class 1259 OID 16640)
-- Name: tipos_ingresso; Type: TABLE; Schema: eventos; Owner: postgres
--

CREATE TABLE eventos.tipos_ingresso (
    id bigint NOT NULL,
    evento_id bigint NOT NULL,
    nome character varying(100) NOT NULL,
    valor numeric(10,2) NOT NULL,
    quant_total integer NOT NULL,
    quant_disp integer NOT NULL,
    status eventos.status_ingresso_tipo DEFAULT 'DISPONIVEL'::eventos.status_ingresso_tipo
);


ALTER TABLE eventos.tipos_ingresso OWNER TO postgres;

--
-- TOC entry 226 (class 1259 OID 16639)
-- Name: tipos_ingresso_id_seq; Type: SEQUENCE; Schema: eventos; Owner: postgres
--

CREATE SEQUENCE eventos.tipos_ingresso_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE eventos.tipos_ingresso_id_seq OWNER TO postgres;

--
-- TOC entry 4414 (class 0 OID 0)
-- Dependencies: 226
-- Name: tipos_ingresso_id_seq; Type: SEQUENCE OWNED BY; Schema: eventos; Owner: postgres
--

ALTER SEQUENCE eventos.tipos_ingresso_id_seq OWNED BY eventos.tipos_ingresso.id;


--
-- TOC entry 235 (class 1259 OID 16728)
-- Name: ingressos; Type: TABLE; Schema: vendas; Owner: postgres
--

CREATE TABLE vendas.ingressos (
    id bigint NOT NULL,
    item_pedido_id bigint NOT NULL,
    codigo_checkin uuid DEFAULT gen_random_uuid(),
    status vendas.status_ingresso DEFAULT 'ATIVO'::vendas.status_ingresso
);


ALTER TABLE vendas.ingressos OWNER TO postgres;

--
-- TOC entry 234 (class 1259 OID 16727)
-- Name: ingressos_id_seq; Type: SEQUENCE; Schema: vendas; Owner: postgres
--

CREATE SEQUENCE vendas.ingressos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE vendas.ingressos_id_seq OWNER TO postgres;

--
-- TOC entry 4415 (class 0 OID 0)
-- Dependencies: 234
-- Name: ingressos_id_seq; Type: SEQUENCE OWNED BY; Schema: vendas; Owner: postgres
--

ALTER SEQUENCE vendas.ingressos_id_seq OWNED BY vendas.ingressos.id;


--
-- TOC entry 233 (class 1259 OID 16697)
-- Name: itens_pedido; Type: TABLE; Schema: vendas; Owner: postgres
--

CREATE TABLE vendas.itens_pedido (
    id bigint NOT NULL,
    pedido_id bigint NOT NULL,
    tipo_ingresso_id bigint NOT NULL,
    quantidade integer NOT NULL,
    valor_uni numeric(10,2) NOT NULL,
    subtotal numeric(10,2) NOT NULL
);


ALTER TABLE vendas.itens_pedido OWNER TO postgres;

--
-- TOC entry 232 (class 1259 OID 16696)
-- Name: itens_pedido_id_seq; Type: SEQUENCE; Schema: vendas; Owner: postgres
--

CREATE SEQUENCE vendas.itens_pedido_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE vendas.itens_pedido_id_seq OWNER TO postgres;

--
-- TOC entry 4416 (class 0 OID 0)
-- Dependencies: 232
-- Name: itens_pedido_id_seq; Type: SEQUENCE OWNED BY; Schema: vendas; Owner: postgres
--

ALTER SEQUENCE vendas.itens_pedido_id_seq OWNED BY vendas.itens_pedido.id;


--
-- TOC entry 237 (class 1259 OID 16752)
-- Name: pagamentos; Type: TABLE; Schema: vendas; Owner: postgres
--

CREATE TABLE vendas.pagamentos (
    id bigint NOT NULL,
    pedido_id bigint NOT NULL,
    gateway character varying(50),
    transaction_id character varying(255),
    pago_em timestamp without time zone,
    valor numeric(10,2),
    status vendas.status_pagamento DEFAULT 'PENDENTE'::vendas.status_pagamento
);


ALTER TABLE vendas.pagamentos OWNER TO postgres;

--
-- TOC entry 236 (class 1259 OID 16751)
-- Name: pagamentos_id_seq; Type: SEQUENCE; Schema: vendas; Owner: postgres
--

CREATE SEQUENCE vendas.pagamentos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE vendas.pagamentos_id_seq OWNER TO postgres;

--
-- TOC entry 4417 (class 0 OID 0)
-- Dependencies: 236
-- Name: pagamentos_id_seq; Type: SEQUENCE OWNED BY; Schema: vendas; Owner: postgres
--

ALTER SEQUENCE vendas.pagamentos_id_seq OWNED BY vendas.pagamentos.id;


--
-- TOC entry 231 (class 1259 OID 16680)
-- Name: pedidos; Type: TABLE; Schema: vendas; Owner: postgres
--

CREATE TABLE vendas.pedidos (
    id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    status vendas.status_pedido DEFAULT 'PENDENTE'::vendas.status_pedido,
    valor_total numeric(10,2) NOT NULL,
    criado_em timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE vendas.pedidos OWNER TO postgres;

--
-- TOC entry 230 (class 1259 OID 16679)
-- Name: pedidos_id_seq; Type: SEQUENCE; Schema: vendas; Owner: postgres
--

CREATE SEQUENCE vendas.pedidos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE vendas.pedidos_id_seq OWNER TO postgres;

--
-- TOC entry 4418 (class 0 OID 0)
-- Dependencies: 230
-- Name: pedidos_id_seq; Type: SEQUENCE OWNED BY; Schema: vendas; Owner: postgres
--

ALTER SEQUENCE vendas.pedidos_id_seq OWNED BY vendas.pedidos.id;


--
-- TOC entry 4218 (class 2604 OID 16601)
-- Name: usuarios id; Type: DEFAULT; Schema: auth; Owner: postgres
--

ALTER TABLE ONLY auth.usuarios ALTER COLUMN id SET DEFAULT nextval('auth.usuarios_id_seq'::regclass);


--
-- TOC entry 4224 (class 2604 OID 16662)
-- Name: checkin id; Type: DEFAULT; Schema: eventos; Owner: postgres
--

ALTER TABLE ONLY eventos.checkin ALTER COLUMN id SET DEFAULT nextval('eventos.checkin_id_seq'::regclass);


--
-- TOC entry 4220 (class 2604 OID 16625)
-- Name: eventos id; Type: DEFAULT; Schema: eventos; Owner: postgres
--

ALTER TABLE ONLY eventos.eventos ALTER COLUMN id SET DEFAULT nextval('eventos.eventos_id_seq'::regclass);


--
-- TOC entry 4222 (class 2604 OID 16643)
-- Name: tipos_ingresso id; Type: DEFAULT; Schema: eventos; Owner: postgres
--

ALTER TABLE ONLY eventos.tipos_ingresso ALTER COLUMN id SET DEFAULT nextval('eventos.tipos_ingresso_id_seq'::regclass);


--
-- TOC entry 4230 (class 2604 OID 16731)
-- Name: ingressos id; Type: DEFAULT; Schema: vendas; Owner: postgres
--

ALTER TABLE ONLY vendas.ingressos ALTER COLUMN id SET DEFAULT nextval('vendas.ingressos_id_seq'::regclass);


--
-- TOC entry 4229 (class 2604 OID 16700)
-- Name: itens_pedido id; Type: DEFAULT; Schema: vendas; Owner: postgres
--

ALTER TABLE ONLY vendas.itens_pedido ALTER COLUMN id SET DEFAULT nextval('vendas.itens_pedido_id_seq'::regclass);


--
-- TOC entry 4233 (class 2604 OID 16755)
-- Name: pagamentos id; Type: DEFAULT; Schema: vendas; Owner: postgres
--

ALTER TABLE ONLY vendas.pagamentos ALTER COLUMN id SET DEFAULT nextval('vendas.pagamentos_id_seq'::regclass);


--
-- TOC entry 4226 (class 2604 OID 16683)
-- Name: pedidos id; Type: DEFAULT; Schema: vendas; Owner: postgres
--

ALTER TABLE ONLY vendas.pedidos ALTER COLUMN id SET DEFAULT nextval('vendas.pedidos_id_seq'::regclass);


--
-- TOC entry 4236 (class 2606 OID 16613)
-- Name: usuarios usuarios_email_key; Type: CONSTRAINT; Schema: auth; Owner: postgres
--

ALTER TABLE ONLY auth.usuarios
    ADD CONSTRAINT usuarios_email_key UNIQUE (email);


--
-- TOC entry 4238 (class 2606 OID 16611)
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: auth; Owner: postgres
--

ALTER TABLE ONLY auth.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id);


--
-- TOC entry 4244 (class 2606 OID 16667)
-- Name: checkin checkin_pkey; Type: CONSTRAINT; Schema: eventos; Owner: postgres
--

ALTER TABLE ONLY eventos.checkin
    ADD CONSTRAINT checkin_pkey PRIMARY KEY (id);


--
-- TOC entry 4240 (class 2606 OID 16633)
-- Name: eventos eventos_pkey; Type: CONSTRAINT; Schema: eventos; Owner: postgres
--

ALTER TABLE ONLY eventos.eventos
    ADD CONSTRAINT eventos_pkey PRIMARY KEY (id);


--
-- TOC entry 4242 (class 2606 OID 16652)
-- Name: tipos_ingresso tipos_ingresso_pkey; Type: CONSTRAINT; Schema: eventos; Owner: postgres
--

ALTER TABLE ONLY eventos.tipos_ingresso
    ADD CONSTRAINT tipos_ingresso_pkey PRIMARY KEY (id);


--
-- TOC entry 4250 (class 2606 OID 16737)
-- Name: ingressos ingressos_pkey; Type: CONSTRAINT; Schema: vendas; Owner: postgres
--

ALTER TABLE ONLY vendas.ingressos
    ADD CONSTRAINT ingressos_pkey PRIMARY KEY (id);


--
-- TOC entry 4248 (class 2606 OID 16708)
-- Name: itens_pedido itens_pedido_pkey; Type: CONSTRAINT; Schema: vendas; Owner: postgres
--

ALTER TABLE ONLY vendas.itens_pedido
    ADD CONSTRAINT itens_pedido_pkey PRIMARY KEY (id);


--
-- TOC entry 4252 (class 2606 OID 16760)
-- Name: pagamentos pagamentos_pkey; Type: CONSTRAINT; Schema: vendas; Owner: postgres
--

ALTER TABLE ONLY vendas.pagamentos
    ADD CONSTRAINT pagamentos_pkey PRIMARY KEY (id);


--
-- TOC entry 4246 (class 2606 OID 16690)
-- Name: pedidos pedidos_pkey; Type: CONSTRAINT; Schema: vendas; Owner: postgres
--

ALTER TABLE ONLY vendas.pedidos
    ADD CONSTRAINT pedidos_pkey PRIMARY KEY (id);


--
-- TOC entry 4253 (class 2606 OID 16653)
-- Name: tipos_ingresso fk_tipo_evento; Type: FK CONSTRAINT; Schema: eventos; Owner: postgres
--

ALTER TABLE ONLY eventos.tipos_ingresso
    ADD CONSTRAINT fk_tipo_evento FOREIGN KEY (evento_id) REFERENCES eventos.eventos(id);


--
-- TOC entry 4257 (class 2606 OID 16738)
-- Name: ingressos fk_ingresso_item; Type: FK CONSTRAINT; Schema: vendas; Owner: postgres
--

ALTER TABLE ONLY vendas.ingressos
    ADD CONSTRAINT fk_ingresso_item FOREIGN KEY (item_pedido_id) REFERENCES vendas.itens_pedido(id);


--
-- TOC entry 4255 (class 2606 OID 16709)
-- Name: itens_pedido fk_item_pedido; Type: FK CONSTRAINT; Schema: vendas; Owner: postgres
--

ALTER TABLE ONLY vendas.itens_pedido
    ADD CONSTRAINT fk_item_pedido FOREIGN KEY (pedido_id) REFERENCES vendas.pedidos(id);


--
-- TOC entry 4256 (class 2606 OID 16714)
-- Name: itens_pedido fk_item_tipo_ingresso; Type: FK CONSTRAINT; Schema: vendas; Owner: postgres
--

ALTER TABLE ONLY vendas.itens_pedido
    ADD CONSTRAINT fk_item_tipo_ingresso FOREIGN KEY (tipo_ingresso_id) REFERENCES eventos.tipos_ingresso(id);


--
-- TOC entry 4258 (class 2606 OID 16761)
-- Name: pagamentos fk_pagamento_pedido; Type: FK CONSTRAINT; Schema: vendas; Owner: postgres
--

ALTER TABLE ONLY vendas.pagamentos
    ADD CONSTRAINT fk_pagamento_pedido FOREIGN KEY (pedido_id) REFERENCES vendas.pedidos(id);


--
-- TOC entry 4254 (class 2606 OID 16691)
-- Name: pedidos fk_pedido_usuario; Type: FK CONSTRAINT; Schema: vendas; Owner: postgres
--

ALTER TABLE ONLY vendas.pedidos
    ADD CONSTRAINT fk_pedido_usuario FOREIGN KEY (usuario_id) REFERENCES auth.usuarios(id);


--
-- PostgreSQL database dump complete
--

\unrestrict 7K2noMQOfxHMmbRbbMAXNoqdP01uZqxeloDXlULThfFZ8ln9eyvoDlzA2905S5V

