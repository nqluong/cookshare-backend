
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;
COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';
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
CREATE TABLE public.activity_logs (
    log_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid,
    activity_type character varying(50) NOT NULL,
    target_id uuid,
    ip_address inet,
    user_agent character varying(255),
    referrer character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_activity_logs_type CHECK (((activity_type)::text = ANY ((ARRAY['VIEW'::character varying, 'CREATE'::character varying, 'UPDATE'::character varying, 'DELETE'::character varying, 'LOGIN'::character varying, 'LOGOUT'::character varying, 'SEARCH'::character varying, 'SHARE'::character varying, 'DOWNLOAD'::character varying, 'PRINT'::character varying])::text[])))
);
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

CREATE TABLE public.collection_recipes (
    collection_id uuid NOT NULL,
    recipe_id uuid NOT NULL,
    added_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);
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
CREATE TABLE public.follows (
    follower_id uuid NOT NULL,
    following_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_follows_not_self CHECK ((follower_id <> following_id))
);
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
    CONSTRAINT chk_notifications_related_type CHECK (((related_type IS NULL) OR ((related_type)::text = ANY ((ARRAY['recipe'::character varying, 'user'::character varying, 'comment'::character varying, 'collection'::character varying])::text[])))),
    CONSTRAINT chk_notifications_type CHECK (((type)::text = ANY ((ARRAY['FOLLOW'::character varying, 'LIKE'::character varying, 'RECIPE_PUBLISHED'::character varying, 'SYSTEM'::character varying, 'MENTION'::character varying, 'SHARE'::character varying, 'RATING'::character varying])::text[])))
);
CREATE TABLE public.recipe_categories (
    recipe_id uuid NOT NULL,
    category_id uuid NOT NULL
);

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
CREATE TABLE public.recipe_likes (
    user_id uuid NOT NULL,
    recipe_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

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
CREATE TABLE public.recipe_tags (
    recipe_id uuid NOT NULL,
    tag_id uuid NOT NULL
);
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
    CONSTRAINT chk_recipes_difficulty CHECK (((difficulty IS NULL) OR ((difficulty)::text = ANY ((ARRAY['EASY'::character varying, 'MEDIUM'::character varying, 'HARD'::character varying, 'EXPERT'::character varying])::text[])))),
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
    CONSTRAINT chk_reports_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'REVIEWING'::character varying, 'RESOLVED'::character varying, 'REJECTED'::character varying, 'CLOSED'::character varying])::text[]))),
    CONSTRAINT chk_reports_type CHECK (((report_type)::text = ANY ((ARRAY['SPAM'::character varying, 'INAPPROPRIATE'::character varying, 'COPYRIGHT'::character varying, 'HARASSMENT'::character varying, 'FAKE'::character varying, 'MISLEADING'::character varying, 'OTHER'::character varying])::text[])))
);
CREATE TABLE public.search_history (
    search_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid,
    search_query text NOT NULL,
    search_type character varying(50),
    result_count integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_search_history_result_count CHECK ((result_count >= 0))
);
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
    CONSTRAINT chk_users_role CHECK (((role)::text = ANY ((ARRAY['USER'::character varying, 'ADMIN'::character varying])::text[])))
);
ALTER TABLE ONLY public.activity_logs ADD CONSTRAINT activity_logs_pkey PRIMARY KEY (log_id);
ALTER TABLE ONLY public.categories ADD CONSTRAINT categories_pkey PRIMARY KEY (category_id);
ALTER TABLE ONLY public.categories ADD CONSTRAINT categories_slug_key UNIQUE (slug);
ALTER TABLE ONLY public.collection_recipes ADD CONSTRAINT collection_recipes_pkey PRIMARY KEY (collection_id, recipe_id);
ALTER TABLE ONLY public.collections ADD CONSTRAINT collections_pkey PRIMARY KEY (collection_id);
ALTER TABLE ONLY public.follows ADD CONSTRAINT follows_pkey PRIMARY KEY (follower_id, following_id);
ALTER TABLE ONLY public.ingredients ADD CONSTRAINT ingredients_pkey PRIMARY KEY (ingredient_id);
ALTER TABLE ONLY public.ingredients ADD CONSTRAINT ingredients_slug_key UNIQUE (slug);
ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (notification_id);
ALTER TABLE ONLY public.recipe_categories
    ADD CONSTRAINT recipe_categories_pkey PRIMARY KEY (recipe_id, category_id);
ALTER TABLE ONLY public.recipe_ingredients
    ADD CONSTRAINT recipe_ingredients_pkey PRIMARY KEY (recipe_id, ingredient_id);
ALTER TABLE ONLY public.recipe_likes
    ADD CONSTRAINT recipe_likes_pkey PRIMARY KEY (user_id, recipe_id);
ALTER TABLE ONLY public.recipe_ratings
    ADD CONSTRAINT recipe_ratings_pkey PRIMARY KEY (rating_id);
ALTER TABLE ONLY public.recipe_steps
    ADD CONSTRAINT recipe_steps_pkey PRIMARY KEY (step_id);
ALTER TABLE ONLY public.recipe_tags
    ADD CONSTRAINT recipe_tags_pkey PRIMARY KEY (recipe_id, tag_id);
ALTER TABLE ONLY public.recipes
    ADD CONSTRAINT recipes_pkey PRIMARY KEY (recipe_id);
ALTER TABLE ONLY public.recipes
    ADD CONSTRAINT recipes_slug_key UNIQUE (slug);
ALTER TABLE ONLY public.reports
    ADD CONSTRAINT reports_pkey PRIMARY KEY (report_id);
ALTER TABLE ONLY public.search_history
    ADD CONSTRAINT search_history_pkey PRIMARY KEY (search_id);
ALTER TABLE ONLY public.tags
    ADD CONSTRAINT tags_pkey PRIMARY KEY (tag_id);
ALTER TABLE ONLY public.tags
    ADD CONSTRAINT tags_slug_key UNIQUE (slug);
ALTER TABLE ONLY public.recipe_steps
    ADD CONSTRAINT unique_recipe_step UNIQUE (recipe_id, step_number);
ALTER TABLE ONLY public.recipe_ratings
    ADD CONSTRAINT unique_user_recipe_rating UNIQUE (user_id, recipe_id);
ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);
ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_facebook_id_key UNIQUE (facebook_id);
ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_google_id_key UNIQUE (google_id);
ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (user_id);
ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);
CREATE INDEX idx_activity_logs_activity_type ON public.activity_logs USING btree (activity_type);
CREATE INDEX idx_activity_logs_created_at ON public.activity_logs USING btree (created_at DESC);
CREATE INDEX idx_activity_logs_user_id ON public.activity_logs USING btree (user_id);
CREATE INDEX idx_categories_is_active ON public.categories USING btree (is_active);
CREATE INDEX idx_categories_parent_id ON public.categories USING btree (parent_id);
CREATE INDEX idx_categories_slug ON public.categories USING btree (slug);
CREATE INDEX idx_collection_recipes_added_at ON public.collection_recipes USING btree (added_at DESC);
CREATE INDEX idx_collection_recipes_recipe_id ON public.collection_recipes USING btree (recipe_id);
CREATE INDEX idx_collections_is_public ON public.collections USING btree (is_public);
CREATE INDEX idx_collections_user_id ON public.collections USING btree (user_id);
CREATE INDEX idx_follows_created_at ON public.follows USING btree (created_at DESC);
CREATE INDEX idx_follows_following_id ON public.follows USING btree (following_id);
CREATE INDEX idx_ingredients_category ON public.ingredients USING btree (category);
CREATE INDEX idx_ingredients_name ON public.ingredients USING btree (name);
CREATE INDEX idx_ingredients_slug ON public.ingredients USING btree (slug);
CREATE INDEX idx_notifications_created_at ON public.notifications USING btree (created_at DESC);
CREATE INDEX idx_notifications_is_read ON public.notifications USING btree (is_read);
CREATE INDEX idx_notifications_type ON public.notifications USING btree (type);
CREATE INDEX idx_notifications_user_id ON public.notifications USING btree (user_id);
CREATE INDEX idx_recipe_categories_category_id ON public.recipe_categories USING btree (category_id);
CREATE INDEX idx_recipe_ingredients_ingredient_id ON public.recipe_ingredients USING btree (ingredient_id);
CREATE INDEX idx_recipe_ingredients_order_index ON public.recipe_ingredients USING btree (order_index);
CREATE INDEX idx_recipe_likes_created_at ON public.recipe_likes USING btree (created_at DESC);
CREATE INDEX idx_recipe_likes_recipe_id ON public.recipe_likes USING btree (recipe_id);
CREATE INDEX idx_recipe_ratings_created_at ON public.recipe_ratings USING btree (created_at DESC);
CREATE INDEX idx_recipe_ratings_recipe_id ON public.recipe_ratings USING btree (recipe_id);
CREATE INDEX idx_recipe_ratings_user_id ON public.recipe_ratings USING btree (user_id);
CREATE INDEX idx_recipe_steps_recipe_id ON public.recipe_steps USING btree (recipe_id);
CREATE INDEX idx_recipe_steps_step_number ON public.recipe_steps USING btree (step_number);
CREATE INDEX idx_recipe_tags_tag_id ON public.recipe_tags USING btree (tag_id);
CREATE INDEX idx_recipes_average_rating ON public.recipes USING btree (average_rating DESC);
CREATE INDEX idx_recipes_created_at ON public.recipes USING btree (created_at DESC);
CREATE INDEX idx_recipes_difficulty ON public.recipes USING btree (difficulty);
CREATE INDEX idx_recipes_is_featured ON public.recipes USING btree (is_featured);
CREATE INDEX idx_recipes_is_published ON public.recipes USING btree (is_published);
CREATE INDEX idx_recipes_like_count ON public.recipes USING btree (like_count DESC);
CREATE INDEX idx_recipes_slug ON public.recipes USING btree (slug);
CREATE INDEX idx_recipes_user_id ON public.recipes USING btree (user_id);
CREATE INDEX idx_recipes_view_count ON public.recipes USING btree (view_count DESC);
CREATE INDEX idx_reports_created_at ON public.reports USING btree (created_at DESC);
CREATE INDEX idx_reports_reporter_id ON public.reports USING btree (reporter_id);
CREATE INDEX idx_reports_reviewed_by ON public.reports USING btree (reviewed_by);
CREATE INDEX idx_reports_status ON public.reports USING btree (status);
CREATE INDEX idx_search_history_created_at ON public.search_history USING btree (created_at DESC);
CREATE INDEX idx_search_history_user_id ON public.search_history USING btree (user_id);
CREATE INDEX idx_tags_is_trending ON public.tags USING btree (is_trending);
CREATE INDEX idx_tags_slug ON public.tags USING btree (slug);
CREATE INDEX idx_users_email ON public.users USING btree (email);
CREATE INDEX idx_users_is_active ON public.users USING btree (is_active);
CREATE INDEX idx_users_role ON public.users USING btree (role);
CREATE INDEX idx_users_username ON public.users USING btree (username);
CREATE TRIGGER update_collections_updated_at BEFORE UPDATE ON public.collections FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_recipe_ratings_updated_at BEFORE UPDATE ON public.recipe_ratings FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_recipes_updated_at BEFORE UPDATE ON public.recipes FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
ALTER TABLE ONLY public.activity_logs
    ADD CONSTRAINT activity_logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.categories(category_id) ON DELETE SET NULL;
ALTER TABLE ONLY public.collection_recipes
    ADD CONSTRAINT collection_recipes_collection_id_fkey FOREIGN KEY (collection_id) REFERENCES public.collections(collection_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.collection_recipes
    ADD CONSTRAINT collection_recipes_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES public.recipes(recipe_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.collections
    ADD CONSTRAINT collections_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.follows
    ADD CONSTRAINT follows_follower_id_fkey FOREIGN KEY (follower_id) REFERENCES public.users(user_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.follows
    ADD CONSTRAINT follows_following_id_fkey FOREIGN KEY (following_id) REFERENCES public.users(user_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.recipe_categories
    ADD CONSTRAINT recipe_categories_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.categories(category_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.recipe_categories
    ADD CONSTRAINT recipe_categories_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES public.recipes(recipe_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.recipe_ingredients
    ADD CONSTRAINT recipe_ingredients_ingredient_id_fkey FOREIGN KEY (ingredient_id) REFERENCES public.ingredients(ingredient_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.recipe_ingredients
    ADD CONSTRAINT recipe_ingredients_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES public.recipes(recipe_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.recipe_likes
    ADD CONSTRAINT recipe_likes_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES public.recipes(recipe_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.recipe_likes
    ADD CONSTRAINT recipe_likes_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.recipe_ratings
    ADD CONSTRAINT recipe_ratings_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES public.recipes(recipe_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.recipe_ratings
    ADD CONSTRAINT recipe_ratings_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.recipe_steps
    ADD CONSTRAINT recipe_steps_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES public.recipes(recipe_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.recipe_tags
    ADD CONSTRAINT recipe_tags_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES public.recipes(recipe_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.recipe_tags
    ADD CONSTRAINT recipe_tags_tag_id_fkey FOREIGN KEY (tag_id) REFERENCES public.tags(tag_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.recipes
    ADD CONSTRAINT recipes_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.reports
    ADD CONSTRAINT reports_recipe_id_fkey FOREIGN KEY (recipe_id) REFERENCES public.recipes(recipe_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.reports
    ADD CONSTRAINT reports_reporter_id_fkey FOREIGN KEY (reporter_id) REFERENCES public.users(user_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.reports
    ADD CONSTRAINT reports_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES public.users(user_id) ON DELETE SET NULL;
ALTER TABLE ONLY public.search_history
    ADD CONSTRAINT search_history_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;






