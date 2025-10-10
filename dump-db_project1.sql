--
-- PostgreSQL database dump
--

-- Dumped from database version 15.13 (Debian 15.13-1.pgdg120+1)
-- Dumped by pg_dump version 17.0

-- Started on 2025-10-10 23:19:42

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
-- TOC entry 2 (class 3079 OID 16385)
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- TOC entry 3660 (class 0 OID 0)
-- Dependencies: 2
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


--
-- TOC entry 243 (class 1255 OID 16396)
-- Name: update_updated_at_column(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_updated_at_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 215 (class 1259 OID 16397)
-- Name: activity_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.activity_logs (
    log_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid,
    activity_type character varying(50) NOT NULL,
    target_id uuid,
    ip_address inet,
    user_agent character varying(255),
    referrer character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_activity_logs_type CHECK (((activity_type)::text = ANY (ARRAY[('VIEW'::character varying)::text, ('CREATE'::character varying)::text, ('UPDATE'::character varying)::text, ('DELETE'::character varying)::text, ('LOGIN'::character varying)::text, ('LOGOUT'::character varying)::text, ('SEARCH'::character varying)::text, ('SHARE'::character varying)::text, ('DOWNLOAD'::character varying)::text, ('PRINT'::character varying)::text])))
);


--
-- TOC entry 216 (class 1259 OID 16405)
-- Name: categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.categories (
    category_id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(255) NOT NULL,
    slug character varying(255),
    description text,
    icon_url character varying(255),
    parent_id uuid,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_categories_name_not_empty CHECK ((length(TRIM(BOTH FROM name)) > 0)),
    CONSTRAINT chk_categories_slug_format CHECK (((slug IS NULL) OR ((slug)::text ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'::text)))
);


--
-- TOC entry 217 (class 1259 OID 16415)
-- Name: collection_recipes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.collection_recipes (
    collection_id uuid NOT NULL,
    recipe_id uuid NOT NULL,
    added_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- TOC entry 218 (class 1259 OID 16419)
-- Name: collections; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.collections (
    collection_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    is_public boolean DEFAULT true,
    cover_image character varying(255),
    recipe_count integer DEFAULT 0,
    view_count integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_collections_name_not_empty CHECK ((length(TRIM(BOTH FROM name)) > 0)),
    CONSTRAINT chk_collections_recipe_count CHECK ((recipe_count >= 0)),
    CONSTRAINT chk_collections_view_count CHECK ((view_count >= 0))
);


--
-- TOC entry 219 (class 1259 OID 16433)
-- Name: follows; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.follows (
    follower_id uuid NOT NULL,
    following_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_follows_not_self CHECK ((follower_id <> following_id))
);


--
-- TOC entry 220 (class 1259 OID 16438)
-- Name: ingredients; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ingredients (
    ingredient_id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(255) NOT NULL,
    slug character varying(255),
    category character varying(100),
    unit character varying(50),
    description text,
    usage_count integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_ingredients_name_not_empty CHECK ((length(TRIM(BOTH FROM name)) > 0)),
    CONSTRAINT chk_ingredients_slug_format CHECK (((slug IS NULL) OR ((slug)::text ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'::text))),
    CONSTRAINT chk_ingredients_usage_count CHECK ((usage_count >= 0))
);


--
-- TOC entry 221 (class 1259 OID 16449)
-- Name: notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notifications (
    notification_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    type character varying(50) NOT NULL,
    title character varying(255),
    message text,
    related_id uuid,
    related_type character varying(50),
    is_read boolean DEFAULT false,
    is_sent boolean DEFAULT false,
    read_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_notifications_read_at CHECK (((read_at IS NULL) OR ((is_read = true) AND (read_at >= created_at)))),
    CONSTRAINT chk_notifications_related_type CHECK (((related_type IS NULL) OR ((related_type)::text = ANY (ARRAY[('recipe'::character varying)::text, ('user'::character varying)::text, ('comment'::character varying)::text, ('collection'::character varying)::text])))),
    CONSTRAINT chk_notifications_type CHECK (((type)::text = ANY (ARRAY[('FOLLOW'::character varying)::text, ('LIKE'::character varying)::text, ('RECIPE_PUBLISHED'::character varying)::text, ('SYSTEM'::character varying)::text, ('MENTION'::character varying)::text, ('SHARE'::character varying)::text, ('RATING'::character varying)::text])))
);


--
-- TOC entry 222 (class 1259 OID 16461)
-- Name: recipe_categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recipe_categories (
    recipe_id uuid NOT NULL,
    category_id uuid NOT NULL
);


--
-- TOC entry 223 (class 1259 OID 16464)
-- Name: recipe_ingredients; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recipe_ingredients (
    recipe_id uuid NOT NULL,
    ingredient_id uuid NOT NULL,
    quantity character varying(50),
    unit character varying(50),
    notes text,
    order_index integer,
    CONSTRAINT chk_recipe_ingredients_order_index CHECK (((order_index IS NULL) OR (order_index >= 0))),
    CONSTRAINT chk_recipe_ingredients_quantity_not_empty CHECK (((quantity IS NULL) OR (length(TRIM(BOTH FROM quantity)) > 0)))
);


--
-- TOC entry 224 (class 1259 OID 16471)
-- Name: recipe_likes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recipe_likes (
    user_id uuid NOT NULL,
    recipe_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- TOC entry 225 (class 1259 OID 16475)
-- Name: recipe_ratings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recipe_ratings (
    rating_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    recipe_id uuid NOT NULL,
    rating integer NOT NULL,
    review text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_recipe_ratings_rating CHECK (((rating >= 1) AND (rating <= 5)))
);


--
-- TOC entry 226 (class 1259 OID 16484)
-- Name: recipe_steps; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recipe_steps (
    step_id uuid DEFAULT gen_random_uuid() NOT NULL,
    recipe_id uuid NOT NULL,
    step_number integer NOT NULL,
    instruction text NOT NULL,
    image_url character varying(255),
    video_url character varying(255),
    estimated_time integer,
    tips text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_recipe_steps_estimated_time CHECK (((estimated_time IS NULL) OR (estimated_time >= 0))),
    CONSTRAINT chk_recipe_steps_instruction_not_empty CHECK ((length(TRIM(BOTH FROM instruction)) > 0)),
    CONSTRAINT chk_recipe_steps_step_number CHECK ((step_number >= 1))
);


--
-- TOC entry 227 (class 1259 OID 16494)
-- Name: recipe_tags; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recipe_tags (
    recipe_id uuid NOT NULL,
    tag_id uuid NOT NULL
);


--
-- TOC entry 228 (class 1259 OID 16497)
-- Name: recipes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recipes (
    recipe_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    slug character varying(255),
    description text,
    prep_time integer,
    cook_time integer,
    servings integer,
    difficulty character varying(50),
    featured_image character varying(255),
    instructions text,
    notes text,
    nutrition_info text,
    view_count integer DEFAULT 0,
    save_count integer DEFAULT 0,
    like_count integer DEFAULT 0,
    average_rating numeric(3,2) DEFAULT 0.00,
    rating_count integer DEFAULT 0,
    is_published boolean DEFAULT false,
    is_featured boolean DEFAULT false,
    meta_keywords character varying(255),
    seasonal_tags character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_recipes_average_rating CHECK (((average_rating >= 0.00) AND (average_rating <= 5.00))),
    CONSTRAINT chk_recipes_cook_time CHECK (((cook_time IS NULL) OR (cook_time >= 0))),
    CONSTRAINT chk_recipes_difficulty CHECK (((difficulty IS NULL) OR ((difficulty)::text = ANY (ARRAY[('EASY'::character varying)::text, ('MEDIUM'::character varying)::text, ('HARD'::character varying)::text, ('EXPERT'::character varying)::text])))),
    CONSTRAINT chk_recipes_like_count CHECK ((like_count >= 0)),
    CONSTRAINT chk_recipes_prep_time CHECK (((prep_time IS NULL) OR (prep_time >= 0))),
    CONSTRAINT chk_recipes_rating_count CHECK ((rating_count >= 0)),
    CONSTRAINT chk_recipes_save_count CHECK ((save_count >= 0)),
    CONSTRAINT chk_recipes_servings CHECK (((servings IS NULL) OR ((servings >= 1) AND (servings <= 100)))),
    CONSTRAINT chk_recipes_slug_format CHECK (((slug IS NULL) OR ((slug)::text ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'::text))),
    CONSTRAINT chk_recipes_title_not_empty CHECK ((length(TRIM(BOTH FROM title)) > 0)),
    CONSTRAINT chk_recipes_total_time CHECK (((prep_time IS NULL) OR (cook_time IS NULL) OR ((prep_time + cook_time) <= 1440))),
    CONSTRAINT chk_recipes_view_count CHECK ((view_count >= 0))
);


--
-- TOC entry 229 (class 1259 OID 16524)
-- Name: reports; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reports (
    report_id uuid DEFAULT gen_random_uuid() NOT NULL,
    reporter_id uuid NOT NULL,
    reported_id uuid,
    report_type character varying(50) NOT NULL,
    reason character varying(255),
    description text,
    status character varying(50) DEFAULT 'PENDING'::character varying,
    admin_note text,
    reviewed_by uuid,
    reviewed_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    recipe_id uuid,
    CONSTRAINT chk_reports_not_self_report CHECK ((reporter_id <> reported_id)),
    CONSTRAINT chk_reports_reviewed_at CHECK (((reviewed_at IS NULL) OR (reviewed_at >= created_at))),
    CONSTRAINT chk_reports_status CHECK (((status)::text = ANY (ARRAY[('PENDING'::character varying)::text, ('REVIEWING'::character varying)::text, ('RESOLVED'::character varying)::text, ('REJECTED'::character varying)::text, ('CLOSED'::character varying)::text]))),
    CONSTRAINT chk_reports_type CHECK (((report_type)::text = ANY (ARRAY[('SPAM'::character varying)::text, ('INAPPROPRIATE'::character varying)::text, ('COPYRIGHT'::character varying)::text, ('HARASSMENT'::character varying)::text, ('FAKE'::character varying)::text, ('MISLEADING'::character varying)::text, ('OTHER'::character varying)::text])))
);


--
-- TOC entry 230 (class 1259 OID 16536)
-- Name: search_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.search_history (
    search_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid,
    search_query text NOT NULL,
    search_type character varying(50),
    result_count integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_search_history_result_count CHECK ((result_count >= 0))
);


--
-- TOC entry 231 (class 1259 OID 16545)
-- Name: tags; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tags (
    tag_id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    slug character varying(100),
    color character varying(20),
    usage_count integer DEFAULT 0,
    is_trending boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_tags_color_format CHECK (((color IS NULL) OR ((color)::text ~ '^#[0-9A-Fa-f]{6}$'::text))),
    CONSTRAINT chk_tags_name_not_empty CHECK ((length(TRIM(BOTH FROM name)) > 0)),
    CONSTRAINT chk_tags_slug_format CHECK (((slug IS NULL) OR ((slug)::text ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'::text))),
    CONSTRAINT chk_tags_usage_count CHECK ((usage_count >= 0))
);


--
-- TOC entry 232 (class 1259 OID 16556)
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    user_id uuid DEFAULT gen_random_uuid() NOT NULL,
    username character varying(100) NOT NULL,
    email character varying(255) NOT NULL,
    full_name character varying(255),
    password_hash character varying(255) NOT NULL,
    avatar_url character varying(255),
    bio text,
    role character varying(50) DEFAULT 'USER'::character varying,
    google_id character varying(100),
    facebook_id character varying(100),
    is_active boolean DEFAULT true,
    email_verified boolean DEFAULT false,
    last_active timestamp without time zone,
    follower_count integer DEFAULT 0,
    following_count integer DEFAULT 0,
    recipe_count integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_users_email_format CHECK (((email)::text ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'::text)),
    CONSTRAINT chk_users_follower_count CHECK ((follower_count >= 0)),
    CONSTRAINT chk_users_following_count CHECK ((following_count >= 0)),
    CONSTRAINT chk_users_recipe_count CHECK ((recipe_count >= 0)),
    CONSTRAINT chk_users_role CHECK (((role)::text = ANY (ARRAY[('USER'::character varying)::text, ('ADMIN'::character varying)::text])))
);


--
-- TOC entry 3379 (class 2606 OID 16576)
-- Name: activity_logs activity_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.activity_logs
    ADD CONSTRAINT activity_logs_pkey PRIMARY KEY (log_id);


--
-- TOC entry 3384 (class 2606 OID 16578)
-- Name: categories categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (category_id);


--
-- TOC entry 3386 (class 2606 OID 16580)
-- Name: categories categories_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_slug_key UNIQUE (slug);


--
-- TOC entry 3391 (class 2606 OID 16582)
-- Name: collection_recipes collection_recipes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collection_recipes
    ADD CONSTRAINT collection_recipes_pkey PRIMARY KEY (collection_id, recipe_id);


--
-- TOC entry 3395 (class 2606 OID 16584)
-- Name: collections collections_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collections
    ADD CONSTRAINT collections_pkey PRIMARY KEY (collection_id);


--
-- TOC entry 3399 (class 2606 OID 16586)
-- Name: follows follows_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.follows
    ADD CONSTRAINT follows_pkey PRIMARY KEY (follower_id, following_id);


--
-- TOC entry 3406 (class 2606 OID 16588)
-- Name: ingredients ingredients_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ingredients
    ADD CONSTRAINT ingredients_pkey PRIMARY KEY (ingredient_id);


--
-- TOC entry 3408 (class 2606 OID 16590)
-- Name: ingredients ingredients_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ingredients
    ADD CONSTRAINT ingredients_slug_key UNIQUE (slug);


--
-- TOC entry 3414 (class 2606 OID 16592)
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (notification_id);


--
-- TOC entry 3417 (class 2606 OID 16594)
-- Name: recipe_categories recipe_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_categories
    ADD CONSTRAINT recipe_categories_pkey PRIMARY KEY (recipe_id, category_id);


--
-- TOC entry 3421 (class 2606 OID 16596)
-- Name: recipe_ingredients recipe_ingredients_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_ingredients
    ADD CONSTRAINT recipe_ingredients_pkey PRIMARY KEY (recipe_id, ingredient_id);


--
-- TOC entry 3425 (class 2606 OID 16598)
-- Name: recipe_likes recipe_likes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_likes
    ADD CONSTRAINT recipe_likes_pkey PRIMARY KEY (user_id, recipe_id);


--
-- TOC entry 3430 (class 2606 OID 16600)
-- Name: recipe_ratings recipe_ratings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_ratings
    ADD CONSTRAINT recipe_ratings_pkey PRIMARY KEY (rating_id);


--
-- TOC entry 3436 (class 2606 OID 16602)
-- Name: recipe_steps recipe_steps_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_steps
    ADD CONSTRAINT recipe_steps_pkey PRIMARY KEY (step_id);


--
-- TOC entry 3441 (class 2606 OID 16604)
-- Name: recipe_tags recipe_tags_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_tags
    ADD CONSTRAINT recipe_tags_pkey PRIMARY KEY (recipe_id, tag_id);


--
-- TOC entry 3452 (class 2606 OID 16606)
-- Name: recipes recipes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipes
    ADD CONSTRAINT recipes_pkey PRIMARY KEY (recipe_id);


--
-- TOC entry 3454 (class 2606 OID 16608)
-- Name: recipes recipes_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipes
    ADD CONSTRAINT recipes_slug_key UNIQUE (slug);


--
-- TOC entry 3460 (class 2606 OID 16610)
-- Name: reports reports_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reports
    ADD CONSTRAINT reports_pkey PRIMARY KEY (report_id);


--
-- TOC entry 3464 (class 2606 OID 16612)
-- Name: search_history search_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.search_history
    ADD CONSTRAINT search_history_pkey PRIMARY KEY (search_id);


--
-- TOC entry 3468 (class 2606 OID 16614)
-- Name: tags tags_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tags
    ADD CONSTRAINT tags_pkey PRIMARY KEY (tag_id);


--
-- TOC entry 3470 (class 2606 OID 16616)
-- Name: tags tags_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tags
    ADD CONSTRAINT tags_slug_key UNIQUE (slug);


--
-- TOC entry 3438 (class 2606 OID 16618)
-- Name: recipe_steps unique_recipe_step; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_steps
    ADD CONSTRAINT unique_recipe_step UNIQUE (recipe_id, step_number);


--
-- TOC entry 3432 (class 2606 OID 16620)
-- Name: recipe_ratings unique_user_recipe_rating; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_ratings
    ADD CONSTRAINT unique_user_recipe_rating UNIQUE (user_id, recipe_id);


--
-- TOC entry 3476 (class 2606 OID 16622)
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- TOC entry 3478 (class 2606 OID 16624)
-- Name: users users_facebook_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_facebook_id_key UNIQUE (facebook_id);


--
-- TOC entry 3480 (class 2606 OID 16626)
-- Name: users users_google_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_google_id_key UNIQUE (google_id);


--
-- TOC entry 3482 (class 2606 OID 16628)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (user_id);


--
-- TOC entry 3484 (class 2606 OID 16630)
-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- TOC entry 3380 (class 1259 OID 16631)
-- Name: idx_activity_logs_activity_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_activity_logs_activity_type ON public.activity_logs USING btree (activity_type);


--
-- TOC entry 3381 (class 1259 OID 16632)
-- Name: idx_activity_logs_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_activity_logs_created_at ON public.activity_logs USING btree (created_at DESC);


--
-- TOC entry 3382 (class 1259 OID 16633)
-- Name: idx_activity_logs_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_activity_logs_user_id ON public.activity_logs USING btree (user_id);


--
-- TOC entry 3387 (class 1259 OID 16634)
-- Name: idx_categories_is_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_categories_is_active ON public.categories USING btree (is_active);


--
-- TOC entry 3388 (class 1259 OID 16635)
-- Name: idx_categories_parent_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_categories_parent_id ON public.categories USING btree (parent_id);


--
-- TOC entry 3389 (class 1259 OID 16636)
-- Name: idx_categories_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_categories_slug ON public.categories USING btree (slug);


--
-- TOC entry 3392 (class 1259 OID 16637)
-- Name: idx_collection_recipes_added_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collection_recipes_added_at ON public.collection_recipes USING btree (added_at DESC);


--
-- TOC entry 3393 (class 1259 OID 16638)
-- Name: idx_collection_recipes_recipe_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collection_recipes_recipe_id ON public.collection_recipes USING btree (recipe_id);


--
-- TOC entry 3396 (class 1259 OID 16639)
-- Name: idx_collections_is_public; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collections_is_public ON public.collections USING btree (is_public);


--
-- TOC entry 3397 (class 1259 OID 16640)
-- Name: idx_collections_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collections_user_id ON public.collections USING btree (user_id);


--
-- TOC entry 3400 (class 1259 OID 16641)
-- Name: idx_follows_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_follows_created_at ON public.follows USING btree (created_at DESC);


--
-- TOC entry 3401 (class 1259 OID 16642)
-- Name: idx_follows_following_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_follows_following_id ON public.follows USING btree (following_id);


--
-- TOC entry 3402 (class 1259 OID 16643)
-- Name: idx_ingredients_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ingredients_category ON public.ingredients USING btree (category);


--
-- TOC entry 3403 (class 1259 OID 16644)
-- Name: idx_ingredients_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ingredients_name ON public.ingredients USING btree (name);


--
-- TOC entry 3404 (class 1259 OID 16645)
-- Name: idx_ingredients_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ingredients_slug ON public.ingredients USING btree (slug);


--
-- TOC entry 3409 (class 1259 OID 16646)
-- Name: idx_notifications_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_created_at ON public.notifications USING btree (created_at DESC);


--
-- TOC entry 3410 (class 1259 OID 16647)
-- Name: idx_notifications_is_read; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_is_read ON public.notifications USING btree (is_read);


--
-- TOC entry 3411 (class 1259 OID 16648)
-- Name: idx_notifications_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_type ON public.notifications USING btree (type);


--
-- TOC entry 3412 (class 1259 OID 16649)
-- Name: idx_notifications_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_user_id ON public.notifications USING btree (user_id);


--
-- TOC entry 3415 (class 1259 OID 16650)
-- Name: idx_recipe_categories_category_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipe_categories_category_id ON public.recipe_categories USING btree (category_id);


--
-- TOC entry 3418 (class 1259 OID 16651)
-- Name: idx_recipe_ingredients_ingredient_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipe_ingredients_ingredient_id ON public.recipe_ingredients USING btree (ingredient_id);


--
-- TOC entry 3419 (class 1259 OID 16652)
-- Name: idx_recipe_ingredients_order_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipe_ingredients_order_index ON public.recipe_ingredients USING btree (order_index);


--
-- TOC entry 3422 (class 1259 OID 16653)
-- Name: idx_recipe_likes_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipe_likes_created_at ON public.recipe_likes USING btree (created_at DESC);


--
-- TOC entry 3423 (class 1259 OID 16654)
-- Name: idx_recipe_likes_recipe_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipe_likes_recipe_id ON public.recipe_likes USING btree (recipe_id);


--
-- TOC entry 3426 (class 1259 OID 16655)
-- Name: idx_recipe_ratings_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipe_ratings_created_at ON public.recipe_ratings USING btree (created_at DESC);


--
-- TOC entry 3427 (class 1259 OID 16656)
-- Name: idx_recipe_ratings_recipe_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipe_ratings_recipe_id ON public.recipe_ratings USING btree (recipe_id);


--
-- TOC entry 3428 (class 1259 OID 16657)
-- Name: idx_recipe_ratings_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipe_ratings_user_id ON public.recipe_ratings USING btree (user_id);


--
-- TOC entry 3433 (class 1259 OID 16658)
-- Name: idx_recipe_steps_recipe_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipe_steps_recipe_id ON public.recipe_steps USING btree (recipe_id);


--
-- TOC entry 3434 (class 1259 OID 16659)
-- Name: idx_recipe_steps_step_number; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipe_steps_step_number ON public.recipe_steps USING btree (step_number);


--
-- TOC entry 3439 (class 1259 OID 16660)
-- Name: idx_recipe_tags_tag_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipe_tags_tag_id ON public.recipe_tags USING btree (tag_id);


--
-- TOC entry 3442 (class 1259 OID 16661)
-- Name: idx_recipes_average_rating; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipes_average_rating ON public.recipes USING btree (average_rating DESC);


--
-- TOC entry 3443 (class 1259 OID 16662)
-- Name: idx_recipes_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipes_created_at ON public.recipes USING btree (created_at DESC);


--
-- TOC entry 3444 (class 1259 OID 16663)
-- Name: idx_recipes_difficulty; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipes_difficulty ON public.recipes USING btree (difficulty);


--
-- TOC entry 3445 (class 1259 OID 16664)
-- Name: idx_recipes_is_featured; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipes_is_featured ON public.recipes USING btree (is_featured);


--
-- TOC entry 3446 (class 1259 OID 16665)
-- Name: idx_recipes_is_published; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipes_is_published ON public.recipes USING btree (is_published);


--
-- TOC entry 3447 (class 1259 OID 16666)
-- Name: idx_recipes_like_count; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipes_like_count ON public.recipes USING btree (like_count DESC);


--
-- TOC entry 3448 (class 1259 OID 16667)
-- Name: idx_recipes_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipes_slug ON public.recipes USING btree (slug);


--
-- TOC entry 3449 (class 1259 OID 16668)
-- Name: idx_recipes_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipes_user_id ON public.recipes USING btree (user_id);


--
-- TOC entry 3450 (class 1259 OID 16669)
-- Name: idx_recipes_view_count; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipes_view_count ON public.recipes USING btree (view_count DESC);


--
-- TOC entry 3455 (class 1259 OID 16670)
-- Name: idx_reports_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_reports_created_at ON public.reports USING btree (created_at DESC);


--
-- TOC entry 3456 (class 1259 OID 16671)
-- Name: idx_reports_reporter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_reports_reporter_id ON public.reports USING btree (reporter_id);


--
-- TOC entry 3457 (class 1259 OID 16672)
-- Name: idx_reports_reviewed_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_reports_reviewed_by ON public.reports USING btree (reviewed_by);


--
-- TOC entry 3458 (class 1259 OID 16673)
-- Name: idx_reports_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_reports_status ON public.reports USING btree (status);


--
-- TOC entry 3461 (class 1259 OID 16674)
-- Name: idx_search_history_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_search_history_created_at ON public.search_history USING btree (created_at DESC);


--
-- TOC entry 3462 (class 1259 OID 16675)
-- Name: idx_search_history_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_search_history_user_id ON public.search_history USING btree (user_id);


--
-- TOC entry 3465 (class 1259 OID 16676)
-- Name: idx_tags_is_trending; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tags_is_trending ON public.tags USING btree (is_trending);


--
-- TOC entry 3466 (class 1259 OID 16677)
-- Name: idx_tags_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tags_slug ON public.tags USING btree (slug);


--
-- TOC entry 3471 (class 1259 OID 16678)
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_email ON public.users USING btree (email);


--
-- TOC entry 3472 (class 1259 OID 16679)
-- Name: idx_users_is_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_is_active ON public.users USING btree (is_active);


--
-- TOC entry 3473 (class 1259 OID 16680)
-- Name: idx_users_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_role ON public.users USING btree (role);


--
-- TOC entry 3474 (class 1259 OID 16681)
-- Name: idx_users_username; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_username ON public.users USING btree (username);


--
-- TOC entry 3509 (class 2620 OID 16682)
-- Name: collections update_collections_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_collections_updated_at BEFORE UPDATE ON public.collections FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 3510 (class 2620 OID 16683)
-- Name: recipe_ratings update_recipe_ratings_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_recipe_ratings_updated_at BEFORE UPDATE ON public.recipe_ratings FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 3511 (class 2620 OID 16684)
-- Name: recipes update_recipes_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_recipes_updated_at BEFORE UPDATE ON public.recipes FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 3512 (class 2620 OID 16685)
-- Name: users update_users_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 3485 (class 2606 OID 16686)
-- Name: activity_logs activity_logs_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.activity_logs
    ADD CONSTRAINT activity_logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


--
-- TOC entry 3486 (class 2606 OID 16691)
-- Name: categories categories_parent_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.categories(category_id) ON DELETE SET NULL;


--
-- TOC entry 3487 (class 2606 OID 16696)
-- Name: collection_recipes collection_recipes_collection_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collection_recipes
    ADD CONSTRAINT collection_recipes_collection_id_fkey FOREIGN KEY (collection_id) REFERENCES public.collections(collection_id) ON DELETE CASCADE;


--
-- TOC entry 3488 (class 2606 OID 16701)
-- Name: collection_recipes collection_recipes_recipe_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collection_recipes
    ADD CONSTRAINT collection_recipes_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES public.recipes(recipe_id) ON DELETE CASCADE;


--
-- TOC entry 3489 (class 2606 OID 16706)
-- Name: collections collections_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collections
    ADD CONSTRAINT collections_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


--
-- TOC entry 3490 (class 2606 OID 16711)
-- Name: follows follows_follower_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.follows
    ADD CONSTRAINT follows_follower_id_fkey FOREIGN KEY (follower_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


--
-- TOC entry 3491 (class 2606 OID 16716)
-- Name: follows follows_following_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.follows
    ADD CONSTRAINT follows_following_id_fkey FOREIGN KEY (following_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


--
-- TOC entry 3492 (class 2606 OID 16721)
-- Name: notifications notifications_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


--
-- TOC entry 3493 (class 2606 OID 16726)
-- Name: recipe_categories recipe_categories_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_categories
    ADD CONSTRAINT recipe_categories_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.categories(category_id) ON DELETE CASCADE;


--
-- TOC entry 3494 (class 2606 OID 16731)
-- Name: recipe_categories recipe_categories_recipe_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_categories
    ADD CONSTRAINT recipe_categories_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES public.recipes(recipe_id) ON DELETE CASCADE;


--
-- TOC entry 3495 (class 2606 OID 16736)
-- Name: recipe_ingredients recipe_ingredients_ingredient_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_ingredients
    ADD CONSTRAINT recipe_ingredients_ingredient_id_fkey FOREIGN KEY (ingredient_id) REFERENCES public.ingredients(ingredient_id) ON DELETE CASCADE;


--
-- TOC entry 3496 (class 2606 OID 16741)
-- Name: recipe_ingredients recipe_ingredients_recipe_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_ingredients
    ADD CONSTRAINT recipe_ingredients_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES public.recipes(recipe_id) ON DELETE CASCADE;


--
-- TOC entry 3497 (class 2606 OID 16746)
-- Name: recipe_likes recipe_likes_recipe_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_likes
    ADD CONSTRAINT recipe_likes_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES public.recipes(recipe_id) ON DELETE CASCADE;


--
-- TOC entry 3498 (class 2606 OID 16751)
-- Name: recipe_likes recipe_likes_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_likes
    ADD CONSTRAINT recipe_likes_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


--
-- TOC entry 3499 (class 2606 OID 16756)
-- Name: recipe_ratings recipe_ratings_recipe_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_ratings
    ADD CONSTRAINT recipe_ratings_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES public.recipes(recipe_id) ON DELETE CASCADE;


--
-- TOC entry 3500 (class 2606 OID 16761)
-- Name: recipe_ratings recipe_ratings_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_ratings
    ADD CONSTRAINT recipe_ratings_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


--
-- TOC entry 3501 (class 2606 OID 16766)
-- Name: recipe_steps recipe_steps_recipe_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_steps
    ADD CONSTRAINT recipe_steps_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES public.recipes(recipe_id) ON DELETE CASCADE;


--
-- TOC entry 3502 (class 2606 OID 16771)
-- Name: recipe_tags recipe_tags_recipe_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_tags
    ADD CONSTRAINT recipe_tags_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES public.recipes(recipe_id) ON DELETE CASCADE;


--
-- TOC entry 3503 (class 2606 OID 16776)
-- Name: recipe_tags recipe_tags_tag_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipe_tags
    ADD CONSTRAINT recipe_tags_tag_id_fkey FOREIGN KEY (tag_id) REFERENCES public.tags(tag_id) ON DELETE CASCADE;


--
-- TOC entry 3504 (class 2606 OID 16781)
-- Name: recipes recipes_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipes
    ADD CONSTRAINT recipes_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


--
-- TOC entry 3505 (class 2606 OID 16786)
-- Name: reports reports_recipe_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reports
    ADD CONSTRAINT reports_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES public.recipes(recipe_id) ON DELETE CASCADE;


--
-- TOC entry 3506 (class 2606 OID 16791)
-- Name: reports reports_reporter_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reports
    ADD CONSTRAINT reports_reporter_id_fkey FOREIGN KEY (reporter_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


--
-- TOC entry 3507 (class 2606 OID 16796)
-- Name: reports reports_reviewed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reports
    ADD CONSTRAINT reports_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES public.users(user_id) ON DELETE SET NULL;


--
-- TOC entry 3508 (class 2606 OID 16801)
-- Name: search_history search_history_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.search_history
    ADD CONSTRAINT search_history_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


-- Completed on 2025-10-10 23:19:42

--
-- PostgreSQL database dump complete
--

