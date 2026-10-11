--
-- PostgreSQL database dump
--

\restrict E8mARnGbhIi7WZfISOvMVF1YKcP3k9MvvozS64YYASadGEeJvvzxnonACJJdurc

-- Dumped from database version 16.15
-- Dumped by pg_dump version 16.15

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

ALTER TABLE IF EXISTS ONLY public.student_subject DROP CONSTRAINT IF EXISTS student_subject_student_id_fkey;
ALTER TABLE IF EXISTS ONLY public.student DROP CONSTRAINT IF EXISTS student_class_group_id_fkey;
ALTER TABLE IF EXISTS ONLY public.schedule_slot DROP CONSTRAINT IF EXISTS schedule_slot_term_id_fkey;
ALTER TABLE IF EXISTS ONLY public.schedule_slot DROP CONSTRAINT IF EXISTS schedule_slot_class_group_id_fkey;
ALTER TABLE IF EXISTS ONLY public.notification DROP CONSTRAINT IF EXISTS notification_template_id_fkey;
ALTER TABLE IF EXISTS ONLY public.monthly_feedback DROP CONSTRAINT IF EXISTS monthly_feedback_student_id_fkey;
ALTER TABLE IF EXISTS ONLY public.monthly_fee DROP CONSTRAINT IF EXISTS monthly_fee_student_id_fkey;
ALTER TABLE IF EXISTS ONLY public.fee_payment DROP CONSTRAINT IF EXISTS fee_payment_monthly_fee_id_fkey;
ALTER TABLE IF EXISTS ONLY public.attendance DROP CONSTRAINT IF EXISTS attendance_student_id_fkey;
DROP INDEX IF EXISTS public.ux_term_single_active;
DROP INDEX IF EXISTS public.ux_admin_user_username;
DROP INDEX IF EXISTS public.ix_student_status;
DROP INDEX IF EXISTS public.ix_student_group;
DROP INDEX IF EXISTS public.ix_student_grade;
DROP INDEX IF EXISTS public.ix_student_full_name;
DROP INDEX IF EXISTS public.ix_slot_term;
DROP INDEX IF EXISTS public.ix_slot_group;
DROP INDEX IF EXISTS public.ix_notification_status;
DROP INDEX IF EXISTS public.ix_notification_created;
DROP INDEX IF EXISTS public.ix_monthly_fee_month;
DROP INDEX IF EXISTS public.ix_feedback_month;
DROP INDEX IF EXISTS public.ix_fee_payment_fee;
DROP INDEX IF EXISTS public.ix_attendance_date;
DROP INDEX IF EXISTS public.flyway_schema_history_s_idx;
ALTER TABLE IF EXISTS ONLY public.notification_template DROP CONSTRAINT IF EXISTS ux_template_name;
ALTER TABLE IF EXISTS ONLY public.student DROP CONSTRAINT IF EXISTS ux_student_code;
ALTER TABLE IF EXISTS ONLY public.monthly_fee DROP CONSTRAINT IF EXISTS ux_monthly_fee_student_month;
ALTER TABLE IF EXISTS ONLY public.grade_rate DROP CONSTRAINT IF EXISTS ux_grade_rate_grade;
ALTER TABLE IF EXISTS ONLY public.monthly_feedback DROP CONSTRAINT IF EXISTS ux_feedback_student_month;
ALTER TABLE IF EXISTS ONLY public.class_group DROP CONSTRAINT IF EXISTS ux_class_group_name;
ALTER TABLE IF EXISTS ONLY public.attendance DROP CONSTRAINT IF EXISTS ux_attendance_student_date;
ALTER TABLE IF EXISTS ONLY public.term DROP CONSTRAINT IF EXISTS term_pkey;
ALTER TABLE IF EXISTS ONLY public.student_subject DROP CONSTRAINT IF EXISTS student_subject_pkey;
ALTER TABLE IF EXISTS ONLY public.student DROP CONSTRAINT IF EXISTS student_pkey;
ALTER TABLE IF EXISTS ONLY public.schedule_slot DROP CONSTRAINT IF EXISTS schedule_slot_pkey;
ALTER TABLE IF EXISTS ONLY public.notification_template DROP CONSTRAINT IF EXISTS notification_template_pkey;
ALTER TABLE IF EXISTS ONLY public.notification DROP CONSTRAINT IF EXISTS notification_pkey;
ALTER TABLE IF EXISTS ONLY public.monthly_feedback DROP CONSTRAINT IF EXISTS monthly_feedback_pkey;
ALTER TABLE IF EXISTS ONLY public.monthly_fee DROP CONSTRAINT IF EXISTS monthly_fee_pkey;
ALTER TABLE IF EXISTS ONLY public.grade_rate DROP CONSTRAINT IF EXISTS grade_rate_pkey;
ALTER TABLE IF EXISTS ONLY public.flyway_schema_history DROP CONSTRAINT IF EXISTS flyway_schema_history_pk;
ALTER TABLE IF EXISTS ONLY public.fee_payment DROP CONSTRAINT IF EXISTS fee_payment_pkey;
ALTER TABLE IF EXISTS ONLY public.class_group DROP CONSTRAINT IF EXISTS class_group_pkey;
ALTER TABLE IF EXISTS ONLY public.attendance DROP CONSTRAINT IF EXISTS attendance_pkey;
ALTER TABLE IF EXISTS ONLY public.admin_user DROP CONSTRAINT IF EXISTS admin_user_pkey;
ALTER TABLE IF EXISTS public.term ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.student ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.schedule_slot ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.notification_template ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.notification ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.monthly_feedback ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.monthly_fee ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.grade_rate ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.fee_payment ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.class_group ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.attendance ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.admin_user ALTER COLUMN id DROP DEFAULT;
DROP SEQUENCE IF EXISTS public.term_id_seq;
DROP TABLE IF EXISTS public.term;
DROP TABLE IF EXISTS public.student_subject;
DROP SEQUENCE IF EXISTS public.student_id_seq;
DROP SEQUENCE IF EXISTS public.student_code_seq;
DROP TABLE IF EXISTS public.student;
DROP SEQUENCE IF EXISTS public.schedule_slot_id_seq;
DROP TABLE IF EXISTS public.schedule_slot;
DROP SEQUENCE IF EXISTS public.notification_template_id_seq;
DROP TABLE IF EXISTS public.notification_template;
DROP SEQUENCE IF EXISTS public.notification_id_seq;
DROP TABLE IF EXISTS public.notification;
DROP SEQUENCE IF EXISTS public.monthly_feedback_id_seq;
DROP TABLE IF EXISTS public.monthly_feedback;
DROP SEQUENCE IF EXISTS public.monthly_fee_id_seq;
DROP TABLE IF EXISTS public.monthly_fee;
DROP SEQUENCE IF EXISTS public.grade_rate_id_seq;
DROP TABLE IF EXISTS public.grade_rate;
DROP TABLE IF EXISTS public.flyway_schema_history;
DROP SEQUENCE IF EXISTS public.fee_payment_id_seq;
DROP TABLE IF EXISTS public.fee_payment;
DROP SEQUENCE IF EXISTS public.class_group_id_seq;
DROP TABLE IF EXISTS public.class_group;
DROP SEQUENCE IF EXISTS public.attendance_id_seq;
DROP TABLE IF EXISTS public.attendance;
DROP SEQUENCE IF EXISTS public.admin_user_id_seq;
DROP TABLE IF EXISTS public.admin_user;
SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: admin_user; Type: TABLE; Schema: public; Owner: portal
--

CREATE TABLE public.admin_user (
    id bigint NOT NULL,
    username character varying(50) NOT NULL,
    password_hash character varying(100) NOT NULL,
    full_name character varying(100),
    enabled boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.admin_user OWNER TO portal;

--
-- Name: admin_user_id_seq; Type: SEQUENCE; Schema: public; Owner: portal
--

CREATE SEQUENCE public.admin_user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.admin_user_id_seq OWNER TO portal;

--
-- Name: admin_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: portal
--

ALTER SEQUENCE public.admin_user_id_seq OWNED BY public.admin_user.id;


--
-- Name: attendance; Type: TABLE; Schema: public; Owner: portal
--

CREATE TABLE public.attendance (
    id bigint NOT NULL,
    student_id bigint NOT NULL,
    class_date date NOT NULL,
    status character varying(10) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT attendance_status_check CHECK (((status)::text = ANY ((ARRAY['PRESENT'::character varying, 'ABSENT'::character varying])::text[])))
);


ALTER TABLE public.attendance OWNER TO portal;

--
-- Name: attendance_id_seq; Type: SEQUENCE; Schema: public; Owner: portal
--

CREATE SEQUENCE public.attendance_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.attendance_id_seq OWNER TO portal;

--
-- Name: attendance_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: portal
--

ALTER SEQUENCE public.attendance_id_seq OWNED BY public.attendance.id;


--
-- Name: class_group; Type: TABLE; Schema: public; Owner: portal
--

CREATE TABLE public.class_group (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.class_group OWNER TO portal;

--
-- Name: class_group_id_seq; Type: SEQUENCE; Schema: public; Owner: portal
--

CREATE SEQUENCE public.class_group_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.class_group_id_seq OWNER TO portal;

--
-- Name: class_group_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: portal
--

ALTER SEQUENCE public.class_group_id_seq OWNED BY public.class_group.id;


--
-- Name: fee_payment; Type: TABLE; Schema: public; Owner: portal
--

CREATE TABLE public.fee_payment (
    id bigint NOT NULL,
    monthly_fee_id bigint NOT NULL,
    amount numeric(10,2) NOT NULL,
    payment_date date NOT NULL,
    method character varying(20) NOT NULL,
    reference character varying(100),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT fee_payment_amount_check CHECK ((amount > (0)::numeric)),
    CONSTRAINT fee_payment_method_check CHECK (((method)::text = ANY ((ARRAY['CASH'::character varying, 'E_TRANSFER'::character varying, 'CHEQUE'::character varying, 'CARD'::character varying, 'OTHER'::character varying])::text[])))
);


ALTER TABLE public.fee_payment OWNER TO portal;

--
-- Name: fee_payment_id_seq; Type: SEQUENCE; Schema: public; Owner: portal
--

CREATE SEQUENCE public.fee_payment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.fee_payment_id_seq OWNER TO portal;

--
-- Name: fee_payment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: portal
--

ALTER SEQUENCE public.fee_payment_id_seq OWNED BY public.fee_payment.id;


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: portal
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO portal;

--
-- Name: grade_rate; Type: TABLE; Schema: public; Owner: portal
--

CREATE TABLE public.grade_rate (
    id bigint NOT NULL,
    grade character varying(30) NOT NULL,
    per_class_rate numeric(10,2) NOT NULL,
    notes text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT grade_rate_per_class_rate_check CHECK ((per_class_rate > (0)::numeric))
);


ALTER TABLE public.grade_rate OWNER TO portal;

--
-- Name: grade_rate_id_seq; Type: SEQUENCE; Schema: public; Owner: portal
--

CREATE SEQUENCE public.grade_rate_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.grade_rate_id_seq OWNER TO portal;

--
-- Name: grade_rate_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: portal
--

ALTER SEQUENCE public.grade_rate_id_seq OWNED BY public.grade_rate.id;


--
-- Name: monthly_fee; Type: TABLE; Schema: public; Owner: portal
--

CREATE TABLE public.monthly_fee (
    id bigint NOT NULL,
    student_id bigint NOT NULL,
    fee_month date NOT NULL,
    rate numeric(10,2) NOT NULL,
    classes_held integer DEFAULT 0 NOT NULL,
    classes_scheduled integer,
    scheduled_override boolean DEFAULT false NOT NULL,
    amount_due numeric(10,2) DEFAULT 0 NOT NULL,
    notes text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT monthly_fee_classes_held_check CHECK ((classes_held >= 0)),
    CONSTRAINT monthly_fee_classes_scheduled_check CHECK ((classes_scheduled >= 0)),
    CONSTRAINT monthly_fee_fee_month_check CHECK ((EXTRACT(day FROM fee_month) = (1)::numeric)),
    CONSTRAINT monthly_fee_rate_check CHECK ((rate >= (0)::numeric))
);


ALTER TABLE public.monthly_fee OWNER TO portal;

--
-- Name: monthly_fee_id_seq; Type: SEQUENCE; Schema: public; Owner: portal
--

CREATE SEQUENCE public.monthly_fee_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.monthly_fee_id_seq OWNER TO portal;

--
-- Name: monthly_fee_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: portal
--

ALTER SEQUENCE public.monthly_fee_id_seq OWNED BY public.monthly_fee.id;


--
-- Name: monthly_feedback; Type: TABLE; Schema: public; Owner: portal
--

CREATE TABLE public.monthly_feedback (
    id bigint NOT NULL,
    student_id bigint NOT NULL,
    feedback_month date NOT NULL,
    topics_taught text NOT NULL,
    improvement_areas text,
    achievements text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT monthly_feedback_feedback_month_check CHECK ((EXTRACT(day FROM feedback_month) = (1)::numeric))
);


ALTER TABLE public.monthly_feedback OWNER TO portal;

--
-- Name: monthly_feedback_id_seq; Type: SEQUENCE; Schema: public; Owner: portal
--

CREATE SEQUENCE public.monthly_feedback_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.monthly_feedback_id_seq OWNER TO portal;

--
-- Name: monthly_feedback_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: portal
--

ALTER SEQUENCE public.monthly_feedback_id_seq OWNED BY public.monthly_feedback.id;


--
-- Name: notification; Type: TABLE; Schema: public; Owner: portal
--

CREATE TABLE public.notification (
    id bigint NOT NULL,
    type character varying(20) NOT NULL,
    channel character varying(10) DEFAULT 'EMAIL'::character varying NOT NULL,
    subject character varying(200) NOT NULL,
    body text NOT NULL,
    recipients text DEFAULT ''::text NOT NULL,
    status character varying(10) NOT NULL,
    scheduled_at timestamp with time zone,
    sent_at timestamp with time zone,
    error_message text,
    template_id bigint,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT notification_channel_check CHECK (((channel)::text = ANY ((ARRAY['EMAIL'::character varying, 'SMS'::character varying])::text[]))),
    CONSTRAINT notification_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'SCHEDULED'::character varying, 'SENT'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT notification_type_check CHECK (((type)::text = ANY ((ARRAY['SCHEDULE'::character varying, 'SCHEDULE_CHANGE'::character varying, 'CLASS_CANCELLED'::character varying, 'FEE_REMINDER'::character varying, 'FEEDBACK'::character varying, 'GENERAL'::character varying])::text[])))
);


ALTER TABLE public.notification OWNER TO portal;

--
-- Name: notification_id_seq; Type: SEQUENCE; Schema: public; Owner: portal
--

CREATE SEQUENCE public.notification_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.notification_id_seq OWNER TO portal;

--
-- Name: notification_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: portal
--

ALTER SEQUENCE public.notification_id_seq OWNED BY public.notification.id;


--
-- Name: notification_template; Type: TABLE; Schema: public; Owner: portal
--

CREATE TABLE public.notification_template (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    type character varying(20) NOT NULL,
    subject character varying(200) NOT NULL,
    body text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT notification_template_type_check CHECK (((type)::text = ANY ((ARRAY['SCHEDULE'::character varying, 'SCHEDULE_CHANGE'::character varying, 'CLASS_CANCELLED'::character varying, 'FEE_REMINDER'::character varying, 'FEEDBACK'::character varying, 'GENERAL'::character varying])::text[])))
);


ALTER TABLE public.notification_template OWNER TO portal;

--
-- Name: notification_template_id_seq; Type: SEQUENCE; Schema: public; Owner: portal
--

CREATE SEQUENCE public.notification_template_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.notification_template_id_seq OWNER TO portal;

--
-- Name: notification_template_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: portal
--

ALTER SEQUENCE public.notification_template_id_seq OWNED BY public.notification_template.id;


--
-- Name: schedule_slot; Type: TABLE; Schema: public; Owner: portal
--

CREATE TABLE public.schedule_slot (
    id bigint NOT NULL,
    term_id bigint NOT NULL,
    class_group_id bigint NOT NULL,
    day_of_week character varying(3) NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    subject character varying(150),
    location character varying(150),
    teacher character varying(100),
    extra_emails text,
    notes text,
    google_event_id character varying(255),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_slot_times CHECK ((end_time > start_time)),
    CONSTRAINT schedule_slot_day_of_week_check CHECK (((day_of_week)::text = ANY ((ARRAY['MON'::character varying, 'TUE'::character varying, 'WED'::character varying, 'THU'::character varying, 'FRI'::character varying, 'SAT'::character varying, 'SUN'::character varying])::text[])))
);


ALTER TABLE public.schedule_slot OWNER TO portal;

--
-- Name: schedule_slot_id_seq; Type: SEQUENCE; Schema: public; Owner: portal
--

CREATE SEQUENCE public.schedule_slot_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.schedule_slot_id_seq OWNER TO portal;

--
-- Name: schedule_slot_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: portal
--

ALTER SEQUENCE public.schedule_slot_id_seq OWNED BY public.schedule_slot.id;


--
-- Name: student; Type: TABLE; Schema: public; Owner: portal
--

CREATE TABLE public.student (
    id bigint NOT NULL,
    student_code character varying(10) NOT NULL,
    full_name character varying(100) NOT NULL,
    date_of_birth date,
    grade character varying(30) NOT NULL,
    school character varying(150),
    address text,
    guardian_name character varying(100) NOT NULL,
    primary_contact character varying(20),
    alternate_contact character varying(20),
    email character varying(150),
    emergency_contact_name character varying(100),
    emergency_contact_phone character varying(20),
    enrollment_date date,
    per_class_rate numeric(10,2) NOT NULL,
    status character varying(10) DEFAULT 'ACTIVE'::character varying NOT NULL,
    class_group_id bigint,
    notes text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    sms_opt_in boolean DEFAULT false NOT NULL,
    CONSTRAINT student_per_class_rate_check CHECK ((per_class_rate >= (0)::numeric)),
    CONSTRAINT student_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);


ALTER TABLE public.student OWNER TO portal;

--
-- Name: student_code_seq; Type: SEQUENCE; Schema: public; Owner: portal
--

CREATE SEQUENCE public.student_code_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.student_code_seq OWNER TO portal;

--
-- Name: student_id_seq; Type: SEQUENCE; Schema: public; Owner: portal
--

CREATE SEQUENCE public.student_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.student_id_seq OWNER TO portal;

--
-- Name: student_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: portal
--

ALTER SEQUENCE public.student_id_seq OWNED BY public.student.id;


--
-- Name: student_subject; Type: TABLE; Schema: public; Owner: portal
--

CREATE TABLE public.student_subject (
    student_id bigint NOT NULL,
    subject character varying(60) NOT NULL
);


ALTER TABLE public.student_subject OWNER TO portal;

--
-- Name: term; Type: TABLE; Schema: public; Owner: portal
--

CREATE TABLE public.term (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    calendar_name character varying(150),
    active boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_term_dates CHECK ((end_date > start_date))
);


ALTER TABLE public.term OWNER TO portal;

--
-- Name: term_id_seq; Type: SEQUENCE; Schema: public; Owner: portal
--

CREATE SEQUENCE public.term_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.term_id_seq OWNER TO portal;

--
-- Name: term_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: portal
--

ALTER SEQUENCE public.term_id_seq OWNED BY public.term.id;


--
-- Name: admin_user id; Type: DEFAULT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.admin_user ALTER COLUMN id SET DEFAULT nextval('public.admin_user_id_seq'::regclass);


--
-- Name: attendance id; Type: DEFAULT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.attendance ALTER COLUMN id SET DEFAULT nextval('public.attendance_id_seq'::regclass);


--
-- Name: class_group id; Type: DEFAULT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.class_group ALTER COLUMN id SET DEFAULT nextval('public.class_group_id_seq'::regclass);


--
-- Name: fee_payment id; Type: DEFAULT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.fee_payment ALTER COLUMN id SET DEFAULT nextval('public.fee_payment_id_seq'::regclass);


--
-- Name: grade_rate id; Type: DEFAULT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.grade_rate ALTER COLUMN id SET DEFAULT nextval('public.grade_rate_id_seq'::regclass);


--
-- Name: monthly_fee id; Type: DEFAULT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.monthly_fee ALTER COLUMN id SET DEFAULT nextval('public.monthly_fee_id_seq'::regclass);


--
-- Name: monthly_feedback id; Type: DEFAULT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.monthly_feedback ALTER COLUMN id SET DEFAULT nextval('public.monthly_feedback_id_seq'::regclass);


--
-- Name: notification id; Type: DEFAULT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.notification ALTER COLUMN id SET DEFAULT nextval('public.notification_id_seq'::regclass);


--
-- Name: notification_template id; Type: DEFAULT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.notification_template ALTER COLUMN id SET DEFAULT nextval('public.notification_template_id_seq'::regclass);


--
-- Name: schedule_slot id; Type: DEFAULT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.schedule_slot ALTER COLUMN id SET DEFAULT nextval('public.schedule_slot_id_seq'::regclass);


--
-- Name: student id; Type: DEFAULT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.student ALTER COLUMN id SET DEFAULT nextval('public.student_id_seq'::regclass);


--
-- Name: term id; Type: DEFAULT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.term ALTER COLUMN id SET DEFAULT nextval('public.term_id_seq'::regclass);


--
-- Data for Name: admin_user; Type: TABLE DATA; Schema: public; Owner: portal
--

COPY public.admin_user (id, username, password_hash, full_name, enabled, created_at, updated_at) FROM stdin;
1	admin	$2a$12$piW4DGEnRtJfBnDw.HPyBOw0UUUilVe4iG1pNXXqTdqfdj.xJv/Ze	Administrator	t	2026-10-06 03:53:58.734708+00	2026-10-06 15:09:02.991028+00
\.


--
-- Data for Name: attendance; Type: TABLE DATA; Schema: public; Owner: portal
--

COPY public.attendance (id, student_id, class_date, status, created_at, updated_at) FROM stdin;
1	1	2026-08-03	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
2	2	2026-08-03	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
3	3	2026-08-03	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
4	4	2026-08-03	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
5	5	2026-08-03	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
6	6	2026-08-03	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
7	1	2026-08-04	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
8	2	2026-08-04	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
9	3	2026-08-04	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
10	4	2026-08-04	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
11	5	2026-08-04	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
12	6	2026-08-04	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
13	1	2026-08-05	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
14	2	2026-08-05	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
15	3	2026-08-05	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
16	4	2026-08-05	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
17	5	2026-08-05	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
18	6	2026-08-05	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
19	1	2026-08-06	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
20	2	2026-08-06	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
21	3	2026-08-06	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
22	4	2026-08-06	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
23	5	2026-08-06	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
24	6	2026-08-06	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
25	1	2026-08-07	ABSENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
26	2	2026-08-07	ABSENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
27	3	2026-08-07	ABSENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
28	4	2026-08-07	ABSENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
29	5	2026-08-07	ABSENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
30	6	2026-08-07	ABSENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
31	1	2026-08-10	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
32	2	2026-08-10	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
33	3	2026-08-10	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
34	4	2026-08-10	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
35	5	2026-08-10	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
36	6	2026-08-10	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
37	1	2026-08-11	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
38	2	2026-08-11	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
39	3	2026-08-11	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
40	4	2026-08-11	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
41	6	2026-08-11	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
42	1	2026-08-12	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
43	2	2026-08-12	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
44	3	2026-08-12	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
45	4	2026-08-12	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
46	6	2026-08-12	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
47	1	2026-08-13	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
48	2	2026-08-13	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
49	3	2026-08-13	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
50	1	2026-08-14	ABSENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
51	2	2026-08-14	ABSENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
52	3	2026-08-14	ABSENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
53	1	2026-08-17	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
54	2	2026-08-17	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
55	3	2026-08-17	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
56	1	2026-08-18	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
57	2	2026-08-18	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
58	3	2026-08-18	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
59	1	2026-08-19	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
60	2	2026-08-19	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
61	2	2026-08-20	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
62	2	2026-08-21	ABSENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
63	2	2026-08-24	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
64	7	2026-07-06	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
65	7	2026-07-07	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
66	7	2026-07-08	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
67	7	2026-07-09	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
68	7	2026-07-10	PRESENT	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
69	9	2026-09-03	PRESENT	2026-10-06 03:55:42.352297+00	2026-10-06 03:55:42.352297+00
70	9	2026-09-04	ABSENT	2026-10-06 03:55:42.355297+00	2026-10-06 03:55:42.355297+00
71	10	2026-09-03	PRESENT	2026-10-06 03:55:42.356471+00	2026-10-06 03:55:42.356471+00
72	10	2026-09-04	ABSENT	2026-10-06 03:55:42.357621+00	2026-10-06 03:55:42.357621+00
73	11	2026-09-02	PRESENT	2026-10-06 03:55:44.924758+00	2026-10-06 03:55:44.924758+00
74	11	2026-09-03	ABSENT	2026-10-06 03:55:44.925923+00	2026-10-06 03:55:44.925923+00
75	12	2026-09-02	PRESENT	2026-10-06 03:55:44.926735+00	2026-10-06 03:55:44.926735+00
76	12	2026-09-03	ABSENT	2026-10-06 03:55:44.92739+00	2026-10-06 03:55:44.92739+00
77	13	2026-09-02	PRESENT	2026-10-06 03:55:44.928047+00	2026-10-06 03:55:44.928047+00
78	13	2026-09-03	ABSENT	2026-10-06 03:55:44.928657+00	2026-10-06 03:55:44.928657+00
79	15	2026-09-01	PRESENT	2026-10-06 03:56:08.184089+00	2026-10-06 03:56:08.184089+00
80	16	2026-09-03	PRESENT	2026-10-06 03:56:22.125785+00	2026-10-06 03:56:22.125785+00
81	16	2026-09-04	ABSENT	2026-10-06 03:56:22.128749+00	2026-10-06 03:56:22.128749+00
82	17	2026-09-03	PRESENT	2026-10-06 03:56:22.130015+00	2026-10-06 03:56:22.130015+00
83	17	2026-09-04	ABSENT	2026-10-06 03:56:22.131037+00	2026-10-06 03:56:22.131037+00
84	18	2026-09-02	PRESENT	2026-10-06 03:56:35.385081+00	2026-10-06 03:56:35.385081+00
85	18	2026-09-03	ABSENT	2026-10-06 03:56:35.385702+00	2026-10-06 03:56:35.385702+00
86	19	2026-09-02	PRESENT	2026-10-06 03:56:35.386164+00	2026-10-06 03:56:35.386164+00
87	19	2026-09-03	ABSENT	2026-10-06 03:56:35.386549+00	2026-10-06 03:56:35.386549+00
88	20	2026-09-02	PRESENT	2026-10-06 03:56:35.38745+00	2026-10-06 03:56:35.38745+00
89	20	2026-09-03	ABSENT	2026-10-06 03:56:35.388433+00	2026-10-06 03:56:35.388433+00
90	23	2026-09-01	PRESENT	2026-10-06 03:56:48.753117+00	2026-10-06 03:56:48.753117+00
91	24	2026-09-03	PRESENT	2026-10-06 03:57:14.671664+00	2026-10-06 03:57:14.671664+00
92	24	2026-09-04	ABSENT	2026-10-06 03:57:14.673553+00	2026-10-06 03:57:14.673553+00
93	25	2026-09-03	PRESENT	2026-10-06 03:57:14.674746+00	2026-10-06 03:57:14.674746+00
94	25	2026-09-04	ABSENT	2026-10-06 03:57:14.675873+00	2026-10-06 03:57:14.675873+00
95	26	2026-09-02	PRESENT	2026-10-06 03:57:17.22644+00	2026-10-06 03:57:17.22644+00
96	26	2026-09-03	ABSENT	2026-10-06 03:57:17.227886+00	2026-10-06 03:57:17.227886+00
97	27	2026-09-02	PRESENT	2026-10-06 03:57:17.228853+00	2026-10-06 03:57:17.228853+00
98	27	2026-09-03	ABSENT	2026-10-06 03:57:17.229775+00	2026-10-06 03:57:17.229775+00
99	28	2026-09-02	PRESENT	2026-10-06 03:57:17.230683+00	2026-10-06 03:57:17.230683+00
100	28	2026-09-03	ABSENT	2026-10-06 03:57:17.231603+00	2026-10-06 03:57:17.231603+00
101	31	2026-09-01	PRESENT	2026-10-06 03:57:29.5902+00	2026-10-06 03:57:29.5902+00
102	32	2026-09-03	PRESENT	2026-10-06 04:00:29.654694+00	2026-10-06 04:00:29.654694+00
103	32	2026-09-04	ABSENT	2026-10-06 04:00:29.657066+00	2026-10-06 04:00:29.657066+00
104	33	2026-09-03	PRESENT	2026-10-06 04:00:29.657628+00	2026-10-06 04:00:29.657628+00
105	33	2026-09-04	ABSENT	2026-10-06 04:00:29.65805+00	2026-10-06 04:00:29.65805+00
106	34	2026-09-02	PRESENT	2026-10-06 04:00:32.77967+00	2026-10-06 04:00:32.77967+00
107	34	2026-09-03	ABSENT	2026-10-06 04:00:32.780426+00	2026-10-06 04:00:32.780426+00
108	35	2026-09-02	PRESENT	2026-10-06 04:00:32.780878+00	2026-10-06 04:00:32.780878+00
109	35	2026-09-03	ABSENT	2026-10-06 04:00:32.781725+00	2026-10-06 04:00:32.781725+00
110	36	2026-09-02	PRESENT	2026-10-06 04:00:32.782148+00	2026-10-06 04:00:32.782148+00
111	36	2026-09-03	ABSENT	2026-10-06 04:00:32.783012+00	2026-10-06 04:00:32.783012+00
112	39	2026-09-01	PRESENT	2026-10-06 04:00:40.14912+00	2026-10-06 04:00:40.14912+00
113	40	2026-09-03	PRESENT	2026-10-06 13:25:54.886941+00	2026-10-06 13:25:54.886941+00
114	40	2026-09-04	ABSENT	2026-10-06 13:25:54.888908+00	2026-10-06 13:25:54.888908+00
115	41	2026-09-03	PRESENT	2026-10-06 13:25:54.889307+00	2026-10-06 13:25:54.889307+00
116	41	2026-09-04	ABSENT	2026-10-06 13:25:54.889574+00	2026-10-06 13:25:54.889574+00
117	42	2026-09-02	PRESENT	2026-10-06 13:25:57.730965+00	2026-10-06 13:25:57.730965+00
118	42	2026-09-03	ABSENT	2026-10-06 13:25:57.731772+00	2026-10-06 13:25:57.731772+00
119	43	2026-09-02	PRESENT	2026-10-06 13:25:57.73225+00	2026-10-06 13:25:57.73225+00
120	43	2026-09-03	ABSENT	2026-10-06 13:25:57.732868+00	2026-10-06 13:25:57.732868+00
121	44	2026-09-02	PRESENT	2026-10-06 13:25:57.733343+00	2026-10-06 13:25:57.733343+00
122	44	2026-09-03	ABSENT	2026-10-06 13:25:57.734245+00	2026-10-06 13:25:57.734245+00
123	47	2026-09-01	PRESENT	2026-10-06 13:26:04.789494+00	2026-10-06 13:26:04.789494+00
124	2	2026-10-05	PRESENT	2026-10-06 13:39:58.935832+00	2026-10-06 13:39:58.935832+00
125	2	2026-10-06	ABSENT	2026-10-06 13:39:58.954275+00	2026-10-06 13:39:58.954275+00
126	3	2026-10-05	PRESENT	2026-10-06 13:39:58.955177+00	2026-10-06 13:39:58.955177+00
127	3	2026-10-06	PRESENT	2026-10-06 13:39:58.955694+00	2026-10-06 13:39:58.955694+00
128	4	2026-10-06	PRESENT	2026-10-06 13:39:58.956106+00	2026-10-06 13:39:58.956106+00
129	5	2026-10-05	PRESENT	2026-10-06 13:39:58.956703+00	2026-10-06 13:39:58.956703+00
130	5	2026-10-06	PRESENT	2026-10-06 13:39:58.957181+00	2026-10-06 13:39:58.957181+00
131	6	2026-10-06	PRESENT	2026-10-06 13:39:58.957568+00	2026-10-06 13:39:58.957568+00
132	6	2026-10-04	PRESENT	2026-10-06 13:39:58.958036+00	2026-10-06 13:39:58.958036+00
133	5	2026-10-02	PRESENT	2026-10-06 13:39:58.958486+00	2026-10-06 13:39:58.958486+00
134	48	2026-09-03	PRESENT	2026-10-06 18:20:04.761016+00	2026-10-06 18:20:04.761016+00
135	48	2026-09-04	ABSENT	2026-10-06 18:20:04.764463+00	2026-10-06 18:20:04.764463+00
136	49	2026-09-03	PRESENT	2026-10-06 18:20:04.764905+00	2026-10-06 18:20:04.764905+00
137	49	2026-09-04	ABSENT	2026-10-06 18:20:04.765331+00	2026-10-06 18:20:04.765331+00
138	50	2026-09-02	PRESENT	2026-10-06 18:20:07.571236+00	2026-10-06 18:20:07.571236+00
139	50	2026-09-03	ABSENT	2026-10-06 18:20:07.571965+00	2026-10-06 18:20:07.571965+00
140	51	2026-09-02	PRESENT	2026-10-06 18:20:07.572618+00	2026-10-06 18:20:07.572618+00
141	51	2026-09-03	ABSENT	2026-10-06 18:20:07.573253+00	2026-10-06 18:20:07.573253+00
142	52	2026-09-02	PRESENT	2026-10-06 18:20:07.573662+00	2026-10-06 18:20:07.573662+00
143	52	2026-09-03	ABSENT	2026-10-06 18:20:07.574091+00	2026-10-06 18:20:07.574091+00
144	55	2026-09-01	PRESENT	2026-10-06 18:20:15.890521+00	2026-10-06 18:20:15.890521+00
145	57	2026-09-03	PRESENT	2026-10-06 21:05:30.785478+00	2026-10-06 21:05:30.785478+00
146	57	2026-09-04	ABSENT	2026-10-06 21:05:30.789328+00	2026-10-06 21:05:30.789328+00
147	58	2026-09-03	PRESENT	2026-10-06 21:05:30.7903+00	2026-10-06 21:05:30.7903+00
148	58	2026-09-04	ABSENT	2026-10-06 21:05:30.790949+00	2026-10-06 21:05:30.790949+00
149	59	2026-09-02	PRESENT	2026-10-06 21:05:33.60948+00	2026-10-06 21:05:33.60948+00
150	59	2026-09-03	ABSENT	2026-10-06 21:05:33.61023+00	2026-10-06 21:05:33.61023+00
151	60	2026-09-02	PRESENT	2026-10-06 21:05:33.610663+00	2026-10-06 21:05:33.610663+00
152	60	2026-09-03	ABSENT	2026-10-06 21:05:33.611089+00	2026-10-06 21:05:33.611089+00
153	61	2026-09-02	PRESENT	2026-10-06 21:05:33.611477+00	2026-10-06 21:05:33.611477+00
154	61	2026-09-03	ABSENT	2026-10-06 21:05:33.612541+00	2026-10-06 21:05:33.612541+00
155	65	2026-09-01	PRESENT	2026-10-06 21:05:41.95488+00	2026-10-06 21:05:41.95488+00
156	66	2026-09-03	PRESENT	2026-10-06 21:05:55.808922+00	2026-10-06 21:05:55.808922+00
157	66	2026-09-04	ABSENT	2026-10-06 21:05:55.811064+00	2026-10-06 21:05:55.811064+00
158	67	2026-09-03	PRESENT	2026-10-06 21:05:55.811872+00	2026-10-06 21:05:55.811872+00
159	67	2026-09-04	ABSENT	2026-10-06 21:05:55.812938+00	2026-10-06 21:05:55.812938+00
160	68	2026-09-02	PRESENT	2026-10-06 21:05:58.552124+00	2026-10-06 21:05:58.552124+00
161	68	2026-09-03	ABSENT	2026-10-06 21:05:58.552819+00	2026-10-06 21:05:58.552819+00
162	69	2026-09-02	PRESENT	2026-10-06 21:05:58.553188+00	2026-10-06 21:05:58.553188+00
163	69	2026-09-03	ABSENT	2026-10-06 21:05:58.55352+00	2026-10-06 21:05:58.55352+00
164	70	2026-09-02	PRESENT	2026-10-06 21:05:58.553815+00	2026-10-06 21:05:58.553815+00
165	70	2026-09-03	ABSENT	2026-10-06 21:05:58.554209+00	2026-10-06 21:05:58.554209+00
166	75	2026-09-01	PRESENT	2026-10-06 21:06:17.818682+00	2026-10-06 21:06:17.818682+00
167	77	2026-09-03	PRESENT	2026-10-06 21:06:44.58303+00	2026-10-06 21:06:44.58303+00
168	77	2026-09-04	ABSENT	2026-10-06 21:06:44.585086+00	2026-10-06 21:06:44.585086+00
169	78	2026-09-03	PRESENT	2026-10-06 21:06:44.586064+00	2026-10-06 21:06:44.586064+00
170	78	2026-09-04	ABSENT	2026-10-06 21:06:44.586646+00	2026-10-06 21:06:44.586646+00
171	79	2026-09-02	PRESENT	2026-10-06 21:06:48.008459+00	2026-10-06 21:06:48.008459+00
172	79	2026-09-03	ABSENT	2026-10-06 21:06:48.009177+00	2026-10-06 21:06:48.009177+00
173	80	2026-09-02	PRESENT	2026-10-06 21:06:48.009545+00	2026-10-06 21:06:48.009545+00
174	80	2026-09-03	ABSENT	2026-10-06 21:06:48.009951+00	2026-10-06 21:06:48.009951+00
175	81	2026-09-02	PRESENT	2026-10-06 21:06:48.010252+00	2026-10-06 21:06:48.010252+00
176	81	2026-09-03	ABSENT	2026-10-06 21:06:48.010546+00	2026-10-06 21:06:48.010546+00
177	86	2026-09-01	PRESENT	2026-10-06 21:06:56.753515+00	2026-10-06 21:06:56.753515+00
178	87	2026-09-03	PRESENT	2026-10-06 21:15:26.384675+00	2026-10-06 21:15:26.384675+00
179	87	2026-09-04	ABSENT	2026-10-06 21:15:26.387635+00	2026-10-06 21:15:26.387635+00
180	88	2026-09-03	PRESENT	2026-10-06 21:15:26.388314+00	2026-10-06 21:15:26.388314+00
181	88	2026-09-04	ABSENT	2026-10-06 21:15:26.389575+00	2026-10-06 21:15:26.389575+00
182	89	2026-09-02	PRESENT	2026-10-06 21:15:29.261312+00	2026-10-06 21:15:29.261312+00
183	89	2026-09-03	ABSENT	2026-10-06 21:15:29.262438+00	2026-10-06 21:15:29.262438+00
184	90	2026-09-02	PRESENT	2026-10-06 21:15:29.262996+00	2026-10-06 21:15:29.262996+00
185	90	2026-09-03	ABSENT	2026-10-06 21:15:29.263443+00	2026-10-06 21:15:29.263443+00
186	91	2026-09-02	PRESENT	2026-10-06 21:15:29.26391+00	2026-10-06 21:15:29.26391+00
187	91	2026-09-03	ABSENT	2026-10-06 21:15:29.264372+00	2026-10-06 21:15:29.264372+00
188	96	2026-09-01	PRESENT	2026-10-06 21:15:39.207753+00	2026-10-06 21:15:39.207753+00
189	97	2026-09-03	PRESENT	2026-10-07 00:18:57.130491+00	2026-10-07 00:18:57.130491+00
190	97	2026-09-04	ABSENT	2026-10-07 00:18:57.133289+00	2026-10-07 00:18:57.133289+00
191	98	2026-09-03	PRESENT	2026-10-07 00:18:57.133788+00	2026-10-07 00:18:57.133788+00
192	98	2026-09-04	ABSENT	2026-10-07 00:18:57.134502+00	2026-10-07 00:18:57.134502+00
193	99	2026-09-02	PRESENT	2026-10-07 00:18:59.875326+00	2026-10-07 00:18:59.875326+00
194	99	2026-09-03	ABSENT	2026-10-07 00:18:59.87612+00	2026-10-07 00:18:59.87612+00
195	100	2026-09-02	PRESENT	2026-10-07 00:18:59.876543+00	2026-10-07 00:18:59.876543+00
196	100	2026-09-03	ABSENT	2026-10-07 00:18:59.87693+00	2026-10-07 00:18:59.87693+00
197	101	2026-09-02	PRESENT	2026-10-07 00:18:59.877295+00	2026-10-07 00:18:59.877295+00
198	101	2026-09-03	ABSENT	2026-10-07 00:18:59.878161+00	2026-10-07 00:18:59.878161+00
199	106	2026-09-01	PRESENT	2026-10-07 00:19:08.84112+00	2026-10-07 00:19:08.84112+00
\.


--
-- Data for Name: class_group; Type: TABLE DATA; Schema: public; Owner: portal
--

COPY public.class_group (id, name, description, created_at, updated_at) FROM stdin;
1	Batch1	Senior batch	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
2	Batch2	Junior batch	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
3	Grade 1-4 Core	Core Math & English for Grades 1-4	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
4	Grade 5-8 Core	Core Math, English and Science for Grades 5-8	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
5	E2E Att ksgj6s	\N	2026-10-06 03:55:41.610889+00	2026-10-06 03:55:41.610889+00
6	E2E Fees 2pfrc1	\N	2026-10-06 03:55:44.88749+00	2026-10-06 03:55:44.88749+00
7	E2E Att fjtkrc	\N	2026-10-06 03:56:21.480373+00	2026-10-06 03:56:21.480373+00
8	E2E Fees dnapu1	\N	2026-10-06 03:56:35.335084+00	2026-10-06 03:56:35.335084+00
9	E2E Att a5w347	\N	2026-10-06 03:57:14.107888+00	2026-10-06 03:57:14.107888+00
10	E2E Fees h38o10	\N	2026-10-06 03:57:17.188097+00	2026-10-06 03:57:17.188097+00
11	E2E Att z5tc9x	\N	2026-10-06 04:00:28.149142+00	2026-10-06 04:00:28.149142+00
12	E2E Fees qfbzd2	\N	2026-10-06 04:00:32.726326+00	2026-10-06 04:00:32.726326+00
13	E2E Att joe6u0	\N	2026-10-06 13:25:54.182606+00	2026-10-06 13:25:54.182606+00
14	E2E Fees ow0ws6	\N	2026-10-06 13:25:57.669082+00	2026-10-06 13:25:57.669082+00
15	E2E Att ew4oq0	\N	2026-10-06 18:20:03.880821+00	2026-10-06 18:20:03.880821+00
16	E2E Fees 8h1khr	\N	2026-10-06 18:20:07.528417+00	2026-10-06 18:20:07.528417+00
17	E2E Att lg83wp	\N	2026-10-06 21:05:29.883471+00	2026-10-06 21:05:29.883471+00
18	E2E Fees abdwe9	\N	2026-10-06 21:05:33.564247+00	2026-10-06 21:05:33.564247+00
19	E2E Att f94bhq	\N	2026-10-06 21:05:55.153158+00	2026-10-06 21:05:55.153158+00
20	E2E Fees b2w3gl	\N	2026-10-06 21:05:58.51215+00	2026-10-06 21:05:58.51215+00
21	E2E Att edxewd	\N	2026-10-06 21:06:43.989028+00	2026-10-06 21:06:43.989028+00
22	E2E Fees c01nk8	\N	2026-10-06 21:06:47.971857+00	2026-10-06 21:06:47.971857+00
23	E2E Att lsf13h	\N	2026-10-06 21:15:25.576278+00	2026-10-06 21:15:25.576278+00
24	E2E Fees ugddmo	\N	2026-10-06 21:15:29.215072+00	2026-10-06 21:15:29.215072+00
25	E2E Att dtsl5p	\N	2026-10-07 00:18:55.50575+00	2026-10-07 00:18:55.50575+00
26	E2E Fees nggr6y	\N	2026-10-07 00:18:59.825467+00	2026-10-07 00:18:59.825467+00
\.


--
-- Data for Name: fee_payment; Type: TABLE DATA; Schema: public; Owner: portal
--

COPY public.fee_payment (id, monthly_fee_id, amount, payment_date, method, reference, created_at, updated_at) FROM stdin;
1	1	260.00	2026-08-31	E_TRANSFER	ET-1001	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
2	2	160.00	2026-08-30	CASH	\N	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
3	3	180.00	2026-08-31	E_TRANSFER	ET-1002	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
4	4	160.00	2026-09-02	CHEQUE	CHQ-204	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
5	5	100.00	2026-08-29	CASH	\N	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
6	6	50.00	2026-08-31	E_TRANSFER	ET-1003	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
7	6	50.00	2026-08-15	CASH	\N	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
8	13	40.00	2026-10-05	CASH	\N	2026-10-06 03:55:45.629721+00	2026-10-06 03:55:45.629721+00
9	14	15.00	2026-10-05	CASH	\N	2026-10-06 03:55:46.183932+00	2026-10-06 03:55:46.183932+00
10	15	50.00	2026-10-05	CASH	\N	2026-10-06 03:55:46.667945+00	2026-10-06 03:55:46.667945+00
11	16	40.00	2026-10-05	CASH	\N	2026-10-06 03:56:36.115305+00	2026-10-06 03:56:36.115305+00
12	17	15.00	2026-10-05	CASH	\N	2026-10-06 03:56:36.595312+00	2026-10-06 03:56:36.595312+00
13	18	50.00	2026-10-05	CASH	\N	2026-10-06 03:56:37.076916+00	2026-10-06 03:56:37.076916+00
14	19	40.00	2026-10-05	CASH	\N	2026-10-06 03:57:17.919805+00	2026-10-06 03:57:17.919805+00
15	20	15.00	2026-10-05	CASH	\N	2026-10-06 03:57:18.401918+00	2026-10-06 03:57:18.401918+00
16	21	50.00	2026-10-05	CASH	\N	2026-10-06 03:57:18.970902+00	2026-10-06 03:57:18.970902+00
17	22	40.00	2026-10-06	CASH	\N	2026-10-06 04:00:33.591577+00	2026-10-06 04:00:33.591577+00
18	23	15.00	2026-10-06	CASH	\N	2026-10-06 04:00:34.152203+00	2026-10-06 04:00:34.152203+00
19	24	50.00	2026-10-06	CASH	\N	2026-10-06 04:00:34.718329+00	2026-10-06 04:00:34.718329+00
20	25	40.00	2026-10-06	CASH	\N	2026-10-06 13:25:58.470397+00	2026-10-06 13:25:58.470397+00
21	26	15.00	2026-10-06	CASH	\N	2026-10-06 13:25:58.949266+00	2026-10-06 13:25:58.949266+00
22	27	50.00	2026-10-06	CASH	\N	2026-10-06 13:25:59.431461+00	2026-10-06 13:25:59.431461+00
23	28	40.00	2026-10-06	CASH	\N	2026-10-06 18:20:08.364765+00	2026-10-06 18:20:08.364765+00
24	29	15.00	2026-10-06	CASH	\N	2026-10-06 18:20:08.922616+00	2026-10-06 18:20:08.922616+00
25	30	50.00	2026-10-06	CASH	\N	2026-10-06 18:20:09.404322+00	2026-10-06 18:20:09.404322+00
26	35	20.00	2026-10-06	E_TRANSFER	\N	2026-10-06 20:44:15.075159+00	2026-10-06 20:44:15.075159+00
27	35	50.00	2026-10-06	E_TRANSFER	\N	2026-10-06 20:44:25.497388+00	2026-10-06 20:44:25.497388+00
28	38	40.00	2026-10-06	CASH	\N	2026-10-06 21:05:34.354021+00	2026-10-06 21:05:34.354021+00
29	39	15.00	2026-10-06	CASH	\N	2026-10-06 21:05:34.913603+00	2026-10-06 21:05:34.913603+00
30	40	50.00	2026-10-06	CASH	\N	2026-10-06 21:05:35.410846+00	2026-10-06 21:05:35.410846+00
31	41	40.00	2026-10-06	CASH	\N	2026-10-06 21:05:59.321375+00	2026-10-06 21:05:59.321375+00
32	42	15.00	2026-10-06	CASH	\N	2026-10-06 21:05:59.885652+00	2026-10-06 21:05:59.885652+00
33	43	50.00	2026-10-06	CASH	\N	2026-10-06 21:06:00.374996+00	2026-10-06 21:06:00.374996+00
34	44	40.00	2026-10-06	CASH	\N	2026-10-06 21:06:48.775745+00	2026-10-06 21:06:48.775745+00
35	45	15.00	2026-10-06	CASH	\N	2026-10-06 21:06:49.368164+00	2026-10-06 21:06:49.368164+00
36	46	50.00	2026-10-06	CASH	\N	2026-10-06 21:06:49.970827+00	2026-10-06 21:06:49.970827+00
37	47	40.00	2026-10-06	CASH	\N	2026-10-06 21:15:30.058321+00	2026-10-06 21:15:30.058321+00
38	48	15.00	2026-10-06	CASH	\N	2026-10-06 21:15:30.607177+00	2026-10-06 21:15:30.607177+00
39	49	50.00	2026-10-06	CASH	\N	2026-10-06 21:15:31.098647+00	2026-10-06 21:15:31.098647+00
40	50	40.00	2026-10-06	CASH	\N	2026-10-07 00:19:00.670029+00	2026-10-07 00:19:00.670029+00
41	51	15.00	2026-10-06	CASH	\N	2026-10-07 00:19:01.227885+00	2026-10-07 00:19:01.227885+00
42	52	50.00	2026-10-06	CASH	\N	2026-10-07 00:19:01.712678+00	2026-10-07 00:19:01.712678+00
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: portal
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	schema	SQL	V1__schema.sql	1599598798	portal	2026-10-05 23:53:56.665235	56	t
2	2	seed dev	SQL	V2__seed_dev.sql	1549653818	portal	2026-10-05 23:53:56.736151	9	t
3	3	sms channel	SQL	V3__sms_channel.sql	-1578060698	portal	2026-10-06 17:05:14.079835	10	t
4	4	optional student details	SQL	V4__optional_student_details.sql	-1917220841	portal	2026-10-06 20:18:44.839264	7	t
\.


--
-- Data for Name: grade_rate; Type: TABLE DATA; Schema: public; Owner: portal
--

COPY public.grade_rate (id, grade, per_class_rate, notes, created_at, updated_at) FROM stdin;
1	SK	10.00	Senior Kindergarten	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
2	Grade 1	15.00	\N	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
3	Grade 2	15.00	\N	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
4	Grade 3	15.00	\N	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
5	Grade 4	15.00	\N	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
6	Grade 5	20.00	\N	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
7	Grade 6	20.00	\N	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
8	Grade 7	20.00	\N	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
9	Grade 8	20.00	\N	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
\.


--
-- Data for Name: monthly_fee; Type: TABLE DATA; Schema: public; Owner: portal
--

COPY public.monthly_fee (id, student_id, fee_month, rate, classes_held, classes_scheduled, scheduled_override, amount_due, notes, created_at, updated_at) FROM stdin;
1	1	2026-08-01	20.00	13	13	t	260.00	Batch1	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
2	2	2026-08-01	10.00	16	16	t	160.00	Batch2	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
3	3	2026-08-01	15.00	12	12	t	180.00	Batch2	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
4	4	2026-08-01	20.00	8	8	t	160.00	Batch1	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
5	5	2026-08-01	15.00	6	6	t	90.00	Batch2	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
6	6	2026-08-01	10.00	8	8	t	80.00	Batch2	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
7	1	2026-09-01	20.00	0	\N	f	0.00	Batch1	2026-10-06 03:55:45.171774+00	2026-10-06 03:55:45.171774+00
8	2	2026-09-01	10.00	0	\N	f	0.00	Batch2	2026-10-06 03:55:45.174526+00	2026-10-06 03:55:45.174526+00
9	3	2026-09-01	15.00	0	\N	f	0.00	Batch2	2026-10-06 03:55:45.175459+00	2026-10-06 03:55:45.175459+00
10	4	2026-09-01	20.00	0	\N	f	0.00	Batch1	2026-10-06 03:55:45.176079+00	2026-10-06 03:55:45.176079+00
11	5	2026-09-01	15.00	0	\N	f	0.00	Batch2	2026-10-06 03:55:45.176759+00	2026-10-06 03:55:45.176759+00
12	6	2026-09-01	10.00	0	\N	f	0.00	Batch2	2026-10-06 03:55:45.17735+00	2026-10-06 03:55:45.17735+00
13	11	2026-09-01	20.00	2	\N	f	40.00	E2E Fees 2pfrc1	2026-10-06 03:55:45.178035+00	2026-10-06 03:55:45.178035+00
14	12	2026-09-01	20.00	2	\N	f	40.00	E2E Fees 2pfrc1	2026-10-06 03:55:45.178756+00	2026-10-06 03:55:45.178756+00
15	13	2026-09-01	20.00	2	\N	f	40.00	E2E Fees 2pfrc1	2026-10-06 03:55:45.179609+00	2026-10-06 03:55:45.179609+00
16	18	2026-09-01	20.00	2	\N	f	40.00	E2E Fees dnapu1	2026-10-06 03:56:35.682814+00	2026-10-06 03:56:35.682814+00
17	19	2026-09-01	20.00	2	\N	f	40.00	E2E Fees dnapu1	2026-10-06 03:56:35.684875+00	2026-10-06 03:56:35.684875+00
18	20	2026-09-01	20.00	2	\N	f	40.00	E2E Fees dnapu1	2026-10-06 03:56:35.685847+00	2026-10-06 03:56:35.685847+00
19	26	2026-09-01	20.00	2	\N	f	40.00	E2E Fees h38o10	2026-10-06 03:57:17.49619+00	2026-10-06 03:57:17.49619+00
20	27	2026-09-01	20.00	2	\N	f	40.00	E2E Fees h38o10	2026-10-06 03:57:17.498863+00	2026-10-06 03:57:17.498863+00
21	28	2026-09-01	20.00	2	\N	f	40.00	E2E Fees h38o10	2026-10-06 03:57:17.500852+00	2026-10-06 03:57:17.500852+00
22	34	2026-09-01	20.00	2	\N	f	40.00	E2E Fees qfbzd2	2026-10-06 04:00:33.118141+00	2026-10-06 04:00:33.118141+00
23	35	2026-09-01	20.00	2	\N	f	40.00	E2E Fees qfbzd2	2026-10-06 04:00:33.12293+00	2026-10-06 04:00:33.12293+00
24	36	2026-09-01	20.00	2	\N	f	40.00	E2E Fees qfbzd2	2026-10-06 04:00:33.124145+00	2026-10-06 04:00:33.124145+00
25	42	2026-09-01	20.00	2	\N	f	40.00	E2E Fees ow0ws6	2026-10-06 13:25:58.032306+00	2026-10-06 13:25:58.032306+00
26	43	2026-09-01	20.00	2	\N	f	40.00	E2E Fees ow0ws6	2026-10-06 13:25:58.03715+00	2026-10-06 13:25:58.03715+00
27	44	2026-09-01	20.00	2	\N	f	40.00	E2E Fees ow0ws6	2026-10-06 13:25:58.037988+00	2026-10-06 13:25:58.037988+00
28	50	2026-09-01	20.00	2	\N	f	40.00	E2E Fees 8h1khr	2026-10-06 18:20:07.87911+00	2026-10-06 18:20:07.87911+00
29	51	2026-09-01	20.00	2	\N	f	40.00	E2E Fees 8h1khr	2026-10-06 18:20:07.887138+00	2026-10-06 18:20:07.887138+00
30	52	2026-09-01	20.00	2	\N	f	40.00	E2E Fees 8h1khr	2026-10-06 18:20:07.887992+00	2026-10-06 18:20:07.887992+00
31	1	2026-10-01	20.00	0	\N	f	0.00	Batch1	2026-10-06 20:25:00.404485+00	2026-10-06 20:25:00.404485+00
32	2	2026-10-01	10.00	2	\N	f	20.00	Batch2	2026-10-06 20:25:00.409467+00	2026-10-06 20:25:00.409467+00
33	3	2026-10-01	15.00	2	\N	f	30.00	Batch2	2026-10-06 20:25:00.410552+00	2026-10-06 20:25:00.410552+00
34	4	2026-10-01	20.00	1	\N	f	20.00	Batch1	2026-10-06 20:25:00.411316+00	2026-10-06 20:25:00.411316+00
35	5	2026-10-01	15.00	3	\N	f	45.00	Batch2	2026-10-06 20:25:00.411956+00	2026-10-06 20:25:00.411956+00
36	6	2026-10-01	10.00	2	\N	f	20.00	Batch2	2026-10-06 20:25:00.412899+00	2026-10-06 20:25:00.412899+00
37	56	2026-10-01	15.00	0	\N	f	0.00	\N	2026-10-06 20:25:00.413798+00	2026-10-06 20:25:00.413798+00
38	59	2026-09-01	20.00	2	\N	f	40.00	E2E Fees abdwe9	2026-10-06 21:05:33.903179+00	2026-10-06 21:05:33.903179+00
39	60	2026-09-01	20.00	2	\N	f	40.00	E2E Fees abdwe9	2026-10-06 21:05:33.907762+00	2026-10-06 21:05:33.907762+00
40	61	2026-09-01	20.00	2	\N	f	40.00	E2E Fees abdwe9	2026-10-06 21:05:33.909598+00	2026-10-06 21:05:33.909598+00
41	68	2026-09-01	20.00	2	\N	f	40.00	E2E Fees b2w3gl	2026-10-06 21:05:58.854659+00	2026-10-06 21:05:58.854659+00
42	69	2026-09-01	20.00	2	\N	f	40.00	E2E Fees b2w3gl	2026-10-06 21:05:58.856967+00	2026-10-06 21:05:58.856967+00
43	70	2026-09-01	20.00	2	\N	f	40.00	E2E Fees b2w3gl	2026-10-06 21:05:58.857889+00	2026-10-06 21:05:58.857889+00
44	79	2026-09-01	20.00	2	\N	f	40.00	E2E Fees c01nk8	2026-10-06 21:06:48.31554+00	2026-10-06 21:06:48.31554+00
45	80	2026-09-01	20.00	2	\N	f	40.00	E2E Fees c01nk8	2026-10-06 21:06:48.31688+00	2026-10-06 21:06:48.31688+00
46	81	2026-09-01	20.00	2	\N	f	40.00	E2E Fees c01nk8	2026-10-06 21:06:48.317915+00	2026-10-06 21:06:48.317915+00
47	89	2026-09-01	20.00	2	\N	f	40.00	E2E Fees ugddmo	2026-10-06 21:15:29.584199+00	2026-10-06 21:15:29.584199+00
48	90	2026-09-01	20.00	2	\N	f	40.00	E2E Fees ugddmo	2026-10-06 21:15:29.588122+00	2026-10-06 21:15:29.588122+00
49	91	2026-09-01	20.00	2	\N	f	40.00	E2E Fees ugddmo	2026-10-06 21:15:29.590329+00	2026-10-06 21:15:29.590329+00
50	99	2026-09-01	20.00	2	\N	f	40.00	E2E Fees nggr6y	2026-10-07 00:19:00.19841+00	2026-10-07 00:19:00.19841+00
51	100	2026-09-01	20.00	2	\N	f	40.00	E2E Fees nggr6y	2026-10-07 00:19:00.201816+00	2026-10-07 00:19:00.201816+00
52	101	2026-09-01	20.00	2	\N	f	40.00	E2E Fees nggr6y	2026-10-07 00:19:00.202435+00	2026-10-07 00:19:00.202435+00
53	107	2026-10-01	15.00	0	\N	f	0.00	\N	2026-10-07 00:19:17.049054+00	2026-10-07 00:19:17.049054+00
\.


--
-- Data for Name: monthly_feedback; Type: TABLE DATA; Schema: public; Owner: portal
--

COPY public.monthly_feedback (id, student_id, feedback_month, topics_taught, improvement_areas, achievements, created_at, updated_at) FROM stdin;
1	1	2026-08-01	Linear equations, ratios and rates	Show all working steps	Scored 92% on the ratios quiz	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
2	2	2026-08-01	Letter sounds, sight words, counting to 50	Pencil grip	Reads ten sight words independently	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
3	3	2026-08-01	Addition with regrouping, short reading passages	Reading comprehension questions	Completed every homework sheet	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
4	4	2026-08-01	Integers, persuasive writing, cells	Paragraph structure	Excellent science presentation	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
5	5	2026-08-01	Multiplication tables 2-5, spelling	Times tables speed	Mastered the 5 times table	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
6	6	2026-08-01	Shapes, colours, phonics	Listening during group time	Wrote first name unaided	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
7	1	2026-10-01	LCM hcf	LCM hcf	LCM hcf	2026-10-06 14:45:33.645249+00	2026-10-06 14:45:33.645249+00
\.


--
-- Data for Name: notification; Type: TABLE DATA; Schema: public; Owner: portal
--

COPY public.notification (id, type, channel, subject, body, recipients, status, scheduled_at, sent_at, error_message, template_id, created_at, updated_at) FROM stdin;
1	GENERAL	EMAIL	E2E hello 79huh2	Dear Morgan Mailtest,\n\nThis is about E2E Student 711321.	e2e.parent.711321@example.com	SENT	\N	2026-10-06 03:55:56.559444+00	\N	\N	2026-10-06 03:55:51.523473+00	2026-10-06 03:55:56.560233+00
2	GENERAL	EMAIL	E2E hello f3xkyu	Dear Morgan Mailtest,\n\nThis is about E2E Student 318647.	e2e.parent.318647@example.com	SENT	\N	2026-10-06 03:56:46.994315+00	\N	\N	2026-10-06 03:56:41.967806+00	2026-10-06 03:56:46.994652+00
3	GENERAL	EMAIL	E2E hello nmvqa9	Dear Morgan Mailtest,\n\nThis is about E2E Student 80980.	e2e.parent.80980@example.com	SENT	\N	2026-10-06 03:57:27.883739+00	\N	\N	2026-10-06 03:57:22.856544+00	2026-10-06 03:57:27.885257+00
4	GENERAL	EMAIL	E2E hello jr2ey4	Dear Morgan Mailtest,\n\nThis is about E2E Student 185932.	e2e.parent.185932@example.com	SENT	\N	2026-10-06 04:00:38.614357+00	\N	\N	2026-10-06 04:00:38.573005+00	2026-10-06 04:00:38.614701+00
5	GENERAL	EMAIL	E2E hello pzq7ck	Dear Morgan Mailtest,\n\nThis is about E2E Student 5549.	e2e.parent.5549@example.com	SENT	\N	2026-10-06 13:26:03.305299+00	\N	\N	2026-10-06 13:26:03.297071+00	2026-10-06 13:26:03.30554+00
6	FEEDBACK	EMAIL	Arjun Test — monthly feedback for October 2026	Dear Priya Test,\n\nHere is Arjun Test's progress report for October 2026.\n\nTopics taught:\nLCM hcf\n\nAreas to improve:\nLCM hcf\n\nAchievements:\nLCM hcf\n\nSmart Start Tutoring	priya.test@example.com	SENT	\N	2026-10-06 14:45:38.933405+00	\N	4	2026-10-06 14:45:38.894866+00	2026-10-06 14:45:38.934004+00
7	FEEDBACK	EMAIL	Arjun Test — monthly feedback for October 2026	Dear Priya Test,\n\nHere is Arjun Test's progress report for October 2026.\n\nTopics taught:\nLCM hcf\n\nAreas to improve:\nLCM hcf\n\nAchievements:\nLCM hcf\n\nSmart Start Tutoring	tarun.gupta1089@gmail.com	SENT	\N	2026-10-06 14:46:38.579192+00	\N	4	2026-10-06 14:46:38.568055+00	2026-10-06 14:46:38.579964+00
8	GENERAL	EMAIL	test email	test email	tarun.gupta1089@gmail.com	SENT	\N	2026-10-06 15:15:35.215518+00	\N	\N	2026-10-06 15:15:35.16927+00	2026-10-06 15:15:35.216025+00
9	GENERAL	EMAIL	E2E hello ed4tm4	Dear Morgan Mailtest,\n\nThis is about E2E Student 581667.	e2e.parent.581667@example.com	SENT	\N	2026-10-06 18:20:14.331707+00	\N	\N	2026-10-06 18:20:14.317759+00	2026-10-06 18:20:14.332034+00
10	GENERAL	EMAIL	E2E hello uz7j4t	Dear Morgan Mailtest,\n\nThis is about E2E Student 204925.	e2e.parent.204925@example.com	SENT	\N	2026-10-06 21:05:39.363044+00	\N	\N	2026-10-06 21:05:39.329745+00	2026-10-06 21:05:39.363586+00
11	GENERAL	EMAIL	E2E hello f570ot	Dear Morgan Mailtest,\n\nThis is about E2E Student 47435.	e2e.parent.47435@example.com	SENT	\N	2026-10-06 21:06:04.254982+00	\N	\N	2026-10-06 21:06:04.244949+00	2026-10-06 21:06:04.255597+00
12	GENERAL	SMS	E2E sms qq20k1	Hi Casey Texter, no class tomorrow. E2E sms qq20k1	+14165550470	SENT	\N	2026-10-06 21:06:05.241698+00	\N	\N	2026-10-06 21:06:05.203838+00	2026-10-06 21:06:05.242004+00
13	GENERAL	SMS	E2E sms qu0l9z	Hi Casey Texter, no class tomorrow. E2E sms qu0l9z	+14165552380	SENT	\N	2026-10-06 21:06:24.896498+00	\N	\N	2026-10-06 21:06:24.891879+00	2026-10-06 21:06:24.896748+00
14	GENERAL	EMAIL	E2E hello mdwkgw	Dear Morgan Mailtest,\n\nThis is about E2E Student 715605.	e2e.parent.715605@example.com	SENT	\N	2026-10-06 21:06:53.889788+00	\N	\N	2026-10-06 21:06:53.882578+00	2026-10-06 21:06:53.890589+00
15	GENERAL	SMS	E2E sms 60obki	Hi Casey Texter, no class tomorrow. E2E sms 60obki	+14165557204	SENT	\N	2026-10-06 21:06:54.823155+00	\N	\N	2026-10-06 21:06:54.815301+00	2026-10-06 21:06:54.823653+00
16	FEE_REMINDER	EMAIL	Fee reminder for Mia Sample — October 2026	Dear Sofia Sample,\n\nThis is a friendly reminder that the tuition fee for Mia Sample for October 2026 is outstanding.\n\nAmount due: $20.00\nBalance remaining: $20.00\n\nIf you have already paid, please disregard this message.\n\nSmart Start Tutoring	sofia.sample@example.com	SENT	\N	2026-10-06 21:07:43.023049+00	\N	2	2026-10-06 21:07:43.009573+00	2026-10-06 21:07:43.02539+00
17	FEE_REMINDER	SMS	SMS fee reminder for Mia Sample — October 2026	Smart Start: friendly reminder, Mia Sample's fee for October 2026 has $20.00 outstanding. Please ignore if already paid.	+19055550102	SENT	\N	2026-10-06 21:07:43.029471+00	\N	\N	2026-10-06 21:07:43.024363+00	2026-10-06 21:07:43.034279+00
18	GENERAL	EMAIL	E2E hello 0lepj9	Dear Morgan Mailtest,\n\nThis is about E2E Student 436244.	e2e.parent.436244@example.com	SENT	\N	2026-10-06 21:15:36.108969+00	\N	\N	2026-10-06 21:15:36.051684+00	2026-10-06 21:15:36.109984+00
19	GENERAL	SMS	E2E sms x746dc	Hi Casey Texter, no class tomorrow. E2E sms x746dc	+14165556646	SENT	\N	2026-10-06 21:15:37.101718+00	\N	\N	2026-10-06 21:15:37.078837+00	2026-10-06 21:15:37.102087+00
20	GENERAL	EMAIL	E2E hello r0zvcb	Dear Morgan Mailtest,\n\nThis is about E2E Student 560742.	e2e.parent.560742@example.com	SENT	\N	2026-10-07 00:19:05.755952+00	\N	\N	2026-10-07 00:19:05.705689+00	2026-10-07 00:19:05.756537+00
21	GENERAL	SMS	E2E sms 6sub7p	Hi Casey Texter, no class tomorrow. E2E sms 6sub7p	+14165552221	SENT	\N	2026-10-07 00:19:06.808764+00	\N	\N	2026-10-07 00:19:06.76539+00	2026-10-07 00:19:06.809166+00
\.


--
-- Data for Name: notification_template; Type: TABLE DATA; Schema: public; Owner: portal
--

COPY public.notification_template (id, name, type, subject, body, created_at, updated_at) FROM stdin;
1	Weekly Schedule	SCHEDULE	{{className}} — weekly class schedule	Dear {{guardianName}},\n\nHere is the weekly timetable for {{className}}:\n\n{{scheduleTable}}\n\nPlease contact us if you have any questions.\n\nSmart Start Tutoring	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
2	Fee Reminder	FEE_REMINDER	Fee reminder for {{studentName}} — {{month}}	Dear {{guardianName}},\n\nThis is a friendly reminder that the tuition fee for {{studentName}} for {{month}} is outstanding.\n\nAmount due: {{amountDue}}\nBalance remaining: {{balance}}\n\nIf you have already paid, please disregard this message.\n\nSmart Start Tutoring	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
3	Class Reminder	SCHEDULE	Reminder: {{className}} class on {{date}}	Dear {{guardianName}},\n\nThis is a reminder that {{className}} has class on {{date}}:\n\n{{scheduleTable}}\n\nSee you there!\n\nSmart Start Tutoring	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
4	Monthly Feedback	FEEDBACK	{{studentName}} — monthly feedback for {{month}}	Dear {{guardianName}},\n\nHere is {{studentName}}'s progress report for {{month}}.\n\n{{feedback}}\n\nSmart Start Tutoring	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
\.


--
-- Data for Name: schedule_slot; Type: TABLE DATA; Schema: public; Owner: portal
--

COPY public.schedule_slot (id, term_id, class_group_id, day_of_week, start_time, end_time, subject, location, teacher, extra_emails, notes, google_event_id, created_at, updated_at) FROM stdin;
1	1	3	THU	17:00:00	18:00:00	Math & English	Caledon Centre	Tarun	\N	Weekly	\N	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
2	1	4	SAT	10:00:00	11:30:00	Math/Eng/Sci	Caledon Centre	Tarun	\N	Weekly	\N	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
\.


--
-- Data for Name: student; Type: TABLE DATA; Schema: public; Owner: portal
--

COPY public.student (id, student_code, full_name, date_of_birth, grade, school, address, guardian_name, primary_contact, alternate_contact, email, emergency_contact_name, emergency_contact_phone, enrollment_date, per_class_rate, status, class_group_id, notes, created_at, updated_at, sms_opt_in) FROM stdin;
3	SS003	Leo Demo	2018-11-05	Grade 2	Oak Ridge Elementary	3 Demo Avenue, Caledon, ON	Hannah Demo	905-555-0103	\N	hannah.demo@example.com	Peter Demo	905-555-0203	2025-10-01	15.00	ACTIVE	2	\N	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00	f
4	SS004	Zara Example	2013-01-22	Grade 7	Maple Public School	4 Example Crescent, Caledon, ON	Aisha Example	905-555-0104	\N	aisha.example@example.com	Omar Example	905-555-0204	2026-01-05	20.00	ACTIVE	1	\N	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00	f
5	SS005	Noah Placeholder	2017-08-18	Grade 3	Cedar Valley School	5 Placeholder Lane, Caledon, ON	Grace Placeholder	905-555-0105	\N	grace.placeholder@example.com	Liam Placeholder	905-555-0205	2026-02-02	15.00	ACTIVE	2	\N	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00	f
6	SS006	Ava Fixture	2020-03-09	SK	Cedar Valley School	6 Fixture Court, Caledon, ON	Emma Fixture	905-555-0106	\N	emma.fixture@example.com	Noel Fixture	905-555-0206	2026-03-02	10.00	ACTIVE	2	\N	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00	f
7	SS007	Ethan Inactive	2016-05-14	Grade 4	Maple Public School	7 Former Way, Caledon, ON	Olivia Inactive	905-555-0107	\N	olivia.inactive@example.com	Sam Inactive	905-555-0207	2025-09-02	15.00	INACTIVE	1	Left in July 2026	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00	f
8	SS008	Lily Former	2019-12-01	Grade 1	Oak Ridge Elementary	8 Former Way, Caledon, ON	Chloe Former	905-555-0108	\N	chloe.former@example.com	Ben Former	905-555-0208	2026-04-06	15.00	INACTIVE	\N	Enrolled but never attended	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00	f
9	SS009	E2E Student 147034	2015-05-05	Grade 5	\N	\N	E2E Guardian 147034	905-555-0000	\N	e2e.parent.147034@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	5	\N	2026-10-06 03:55:41.67784+00	2026-10-06 03:55:42.685644+00	f
10	SS010	E2E Student 841622	2015-05-05	Grade 5	\N	\N	E2E Guardian 841622	905-555-0000	\N	e2e.parent.841622@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	5	\N	2026-10-06 03:55:41.708992+00	2026-10-06 03:55:42.709783+00	f
2	SS002	Mia Sample	2020-06-30	SK	Oak Ridge Elementary	2 Sample Road, Caledon, ON	Sofia Sample	905-555-0102	905-555-0302	sofia.sample@example.com	Marco Sample	905-555-0202	2025-09-02	10.00	ACTIVE	2	\N	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00	f
11	SS011	E2E Student 20680	2015-05-05	Grade 5	\N	\N	E2E Guardian 20680	905-555-0000	\N	e2e.parent.20680@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	6	\N	2026-10-06 03:55:44.899541+00	2026-10-06 03:55:50.959565+00	f
12	SS012	E2E Student 139418	2015-05-05	Grade 5	\N	\N	E2E Guardian 139418	905-555-0000	\N	e2e.parent.139418@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	6	\N	2026-10-06 03:55:44.908163+00	2026-10-06 03:55:50.973452+00	f
13	SS013	E2E Student 743813	2015-05-05	Grade 5	\N	\N	E2E Guardian 743813	905-555-0000	\N	e2e.parent.743813@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	6	\N	2026-10-06 03:55:44.915924+00	2026-10-06 03:55:50.986434+00	f
15	SS015	E2E Student 861831	2015-05-05	Grade 5	\N	\N	E2E Guardian 861831	905-555-0000	\N	e2e.parent.861831@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	\N	\N	2026-10-06 03:56:08.160884+00	2026-10-06 03:56:08.721953+00	f
16	SS016	E2E Student 148930	2015-05-05	Grade 5	\N	\N	E2E Guardian 148930	905-555-0000	\N	e2e.parent.148930@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	7	\N	2026-10-06 03:56:21.504792+00	2026-10-06 03:56:22.447698+00	f
17	SS017	E2E Student 808292	2015-05-05	Grade 5	\N	\N	E2E Guardian 808292	905-555-0000	\N	e2e.parent.808292@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	7	\N	2026-10-06 03:56:21.522615+00	2026-10-06 03:56:22.467216+00	f
18	SS018	E2E Student 667734	2015-05-05	Grade 5	\N	\N	E2E Guardian 667734	905-555-0000	\N	e2e.parent.667734@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	8	\N	2026-10-06 03:56:35.349618+00	2026-10-06 03:56:41.362061+00	f
19	SS019	E2E Student 631901	2015-05-05	Grade 5	\N	\N	E2E Guardian 631901	905-555-0000	\N	e2e.parent.631901@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	8	\N	2026-10-06 03:56:35.362004+00	2026-10-06 03:56:41.379043+00	f
20	SS020	E2E Student 219320	2015-05-05	Grade 5	\N	\N	E2E Guardian 219320	905-555-0000	\N	e2e.parent.219320@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	8	\N	2026-10-06 03:56:35.373039+00	2026-10-06 03:56:41.39616+00	f
23	SS023	E2E Student 225689	2015-05-05	Grade 5	\N	\N	E2E Guardian 225689	905-555-0000	\N	e2e.parent.225689@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	\N	\N	2026-10-06 03:56:48.740012+00	2026-10-06 03:56:49.075781+00	f
24	SS024	E2E Student 844565	2015-05-05	Grade 5	\N	\N	E2E Guardian 844565	905-555-0000	\N	e2e.parent.844565@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	9	\N	2026-10-06 03:57:14.127199+00	2026-10-06 03:57:14.958377+00	f
25	SS025	E2E Student 430758	2015-05-05	Grade 5	\N	\N	E2E Guardian 430758	905-555-0000	\N	e2e.parent.430758@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	9	\N	2026-10-06 03:57:14.141631+00	2026-10-06 03:57:14.975962+00	f
26	SS026	E2E Student 966670	2015-05-05	Grade 5	\N	\N	E2E Guardian 966670	905-555-0000	\N	e2e.parent.966670@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	10	\N	2026-10-06 03:57:17.197512+00	2026-10-06 03:57:22.284472+00	f
27	SS027	E2E Student 88220	2015-05-05	Grade 5	\N	\N	E2E Guardian 88220	905-555-0000	\N	e2e.parent.88220@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	10	\N	2026-10-06 03:57:17.207208+00	2026-10-06 03:57:22.303042+00	f
28	SS028	E2E Student 873562	2015-05-05	Grade 5	\N	\N	E2E Guardian 873562	905-555-0000	\N	e2e.parent.873562@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	10	\N	2026-10-06 03:57:17.216511+00	2026-10-06 03:57:22.321717+00	f
32	SS032	E2E Student 658809	2015-05-05	Grade 5	\N	\N	E2E Guardian 658809	905-555-0000	\N	e2e.parent.658809@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	11	\N	2026-10-06 04:00:28.232335+00	2026-10-06 04:00:30.023983+00	f
31	SS031	E2E Student 758844	2015-05-05	Grade 5	\N	\N	E2E Guardian 758844	905-555-0000	\N	e2e.parent.758844@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	\N	\N	2026-10-06 03:57:29.577475+00	2026-10-06 03:57:29.870262+00	f
33	SS033	E2E Student 928317	2015-05-05	Grade 5	\N	\N	E2E Guardian 928317	905-555-0000	\N	e2e.parent.928317@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	11	\N	2026-10-06 04:00:28.257668+00	2026-10-06 04:00:30.046598+00	f
34	SS034	E2E Student 399134	2015-05-05	Grade 5	\N	\N	E2E Guardian 399134	905-555-0000	\N	e2e.parent.399134@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	12	\N	2026-10-06 04:00:32.740731+00	2026-10-06 04:00:37.95359+00	f
35	SS035	E2E Student 49652	2015-05-05	Grade 5	\N	\N	E2E Guardian 49652	905-555-0000	\N	e2e.parent.49652@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	12	\N	2026-10-06 04:00:32.75493+00	2026-10-06 04:00:37.969647+00	f
36	SS036	E2E Student 801235	2015-05-05	Grade 5	\N	\N	E2E Guardian 801235	905-555-0000	\N	e2e.parent.801235@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	12	\N	2026-10-06 04:00:32.767284+00	2026-10-06 04:00:37.983742+00	f
39	SS039	E2E Student 3154	2015-05-05	Grade 5	\N	\N	E2E Guardian 3154	905-555-0000	\N	e2e.parent.3154@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	\N	\N	2026-10-06 04:00:40.138806+00	2026-10-06 04:00:40.487344+00	f
40	SS040	E2E Student 670268	2015-05-05	Grade 5	\N	\N	E2E Guardian 670268	905-555-0000	\N	e2e.parent.670268@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	13	\N	2026-10-06 13:25:54.224923+00	2026-10-06 13:25:55.226986+00	f
41	SS041	E2E Student 140342	2015-05-05	Grade 5	\N	\N	E2E Guardian 140342	905-555-0000	\N	e2e.parent.140342@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	13	\N	2026-10-06 13:25:54.24249+00	2026-10-06 13:25:55.242762+00	f
1	SS001	Arjun Test	2013-04-12	Grade 7	Maple Public School	1 Test Street, Caledon, ON	Priya Test	905-555-0101	\N	tarun.gupta1089@gmail.com	Ravi Test	905-555-0201	2025-09-02	20.00	ACTIVE	1	\N	2026-10-06 03:53:56.743007+00	2026-10-06 14:46:04.078418+00	f
86	SS086	E2E Student 210422	2015-05-05	Grade 5	\N	\N	E2E Guardian 210422	905-555-0000	\N	e2e.parent.210422@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	\N	\N	2026-10-06 21:06:56.743362+00	2026-10-06 21:06:57.052999+00	f
43	SS043	E2E Student 708543	2015-05-05	Grade 5	\N	\N	E2E Guardian 708543	905-555-0000	\N	e2e.parent.708543@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	14	\N	2026-10-06 13:25:57.696018+00	2026-10-06 13:26:02.762642+00	f
90	SS090	E2E Student 884564	2015-05-05	Grade 5	\N	\N	E2E Guardian 884564	905-555-0000	\N	e2e.parent.884564@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	24	\N	2026-10-06 21:15:29.238349+00	2026-10-06 21:15:35.426628+00	f
97	SS097	E2E Student 298450	2015-05-05	Grade 5	\N	\N	E2E Guardian 298450	905-555-0000	\N	e2e.parent.298450@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	25	\N	2026-10-07 00:18:55.608612+00	2026-10-07 00:18:57.466819+00	f
98	SS098	E2E Student 234141	2015-05-05	Grade 5	\N	\N	E2E Guardian 234141	905-555-0000	\N	e2e.parent.234141@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	25	\N	2026-10-07 00:18:55.642064+00	2026-10-07 00:18:57.487809+00	f
99	SS099	E2E Student 120746	2015-05-05	Grade 5	\N	\N	E2E Guardian 120746	905-555-0000	\N	e2e.parent.120746@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	26	\N	2026-10-07 00:18:59.838354+00	2026-10-07 00:19:05.055965+00	f
101	SS101	E2E Student 149499	2015-05-05	Grade 5	\N	\N	E2E Guardian 149499	905-555-0000	\N	e2e.parent.149499@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	26	\N	2026-10-07 00:18:59.862934+00	2026-10-07 00:19:05.084183+00	f
42	SS042	E2E Student 667675	2015-05-05	Grade 5	\N	\N	E2E Guardian 667675	905-555-0000	\N	e2e.parent.667675@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	14	\N	2026-10-06 13:25:57.684615+00	2026-10-06 13:26:02.74854+00	f
44	SS044	E2E Student 29223	2015-05-05	Grade 5	\N	\N	E2E Guardian 29223	905-555-0000	\N	e2e.parent.29223@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	14	\N	2026-10-06 13:25:57.716628+00	2026-10-06 13:26:02.773547+00	f
47	SS047	E2E Student 43789	2015-05-05	Grade 5	\N	\N	E2E Guardian 43789	905-555-0000	\N	e2e.parent.43789@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	\N	\N	2026-10-06 13:26:04.780484+00	2026-10-06 13:26:05.078383+00	f
87	SS087	E2E Student 573723	2015-05-05	Grade 5	\N	\N	E2E Guardian 573723	905-555-0000	\N	e2e.parent.573723@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	23	\N	2026-10-06 21:15:25.663005+00	2026-10-06 21:15:26.721398+00	f
48	SS048	E2E Student 33852	2015-05-05	Grade 5	\N	\N	E2E Guardian 33852	905-555-0000	\N	e2e.parent.33852@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	15	\N	2026-10-06 18:20:04.011203+00	2026-10-06 18:20:05.088197+00	f
49	SS049	E2E Student 656452	2015-05-05	Grade 5	\N	\N	E2E Guardian 656452	905-555-0000	\N	e2e.parent.656452@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	15	\N	2026-10-06 18:20:04.036846+00	2026-10-06 18:20:05.103633+00	f
88	SS088	E2E Student 459703	2015-05-05	Grade 5	\N	\N	E2E Guardian 459703	905-555-0000	\N	e2e.parent.459703@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	23	\N	2026-10-06 21:15:25.69048+00	2026-10-06 21:15:26.743296+00	f
89	SS089	E2E Student 832237	2015-05-05	Grade 5	\N	\N	E2E Guardian 832237	905-555-0000	\N	e2e.parent.832237@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	24	\N	2026-10-06 21:15:29.227611+00	2026-10-06 21:15:35.409693+00	f
91	SS091	E2E Student 16457	2015-05-05	Grade 5	\N	\N	E2E Guardian 16457	905-555-0000	\N	e2e.parent.16457@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	24	\N	2026-10-06 21:15:29.249395+00	2026-10-06 21:15:35.440924+00	f
50	SS050	E2E Student 251962	2015-05-05	Grade 5	\N	\N	E2E Guardian 251962	905-555-0000	\N	e2e.parent.251962@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	16	\N	2026-10-06 18:20:07.540952+00	2026-10-06 18:20:13.702454+00	f
51	SS051	E2E Student 324139	2015-05-05	Grade 5	\N	\N	E2E Guardian 324139	905-555-0000	\N	e2e.parent.324139@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	16	\N	2026-10-06 18:20:07.551809+00	2026-10-06 18:20:13.718363+00	f
52	SS052	E2E Student 236382	2015-05-05	Grade 5	\N	\N	E2E Guardian 236382	905-555-0000	\N	e2e.parent.236382@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	16	\N	2026-10-06 18:20:07.561402+00	2026-10-06 18:20:13.730942+00	f
96	SS096	E2E Student 475140	2015-05-05	Grade 5	\N	\N	E2E Guardian 475140	905-555-0000	\N	e2e.parent.475140@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	\N	\N	2026-10-06 21:15:39.19405+00	2026-10-06 21:15:39.517461+00	f
55	SS055	E2E Student 514113	2015-05-05	Grade 5	\N	\N	E2E Guardian 514113	905-555-0000	\N	e2e.parent.514113@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	\N	\N	2026-10-06 18:20:15.880849+00	2026-10-06 18:20:16.212069+00	f
56	SS056	avyansh	2026-10-01	Grade 1	\N	\N	sdf	6478062610	\N	tarunabc@gmail.com	tarun	6478062610	2026-10-06	15.00	ACTIVE	\N	\N	2026-10-06 20:24:41.241508+00	2026-10-06 20:24:41.241508+00	f
100	SS100	E2E Student 461285	2015-05-05	Grade 5	\N	\N	E2E Guardian 461285	905-555-0000	\N	e2e.parent.461285@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	26	\N	2026-10-07 00:18:59.851537+00	2026-10-07 00:19:05.070892+00	f
57	SS057	E2E Student 893474	2015-05-05	Grade 5	\N	\N	E2E Guardian 893474	905-555-0000	\N	e2e.parent.893474@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	17	\N	2026-10-06 21:05:30.006273+00	2026-10-06 21:05:31.115031+00	f
58	SS058	E2E Student 329806	2015-05-05	Grade 5	\N	\N	E2E Guardian 329806	905-555-0000	\N	e2e.parent.329806@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	17	\N	2026-10-06 21:05:30.034339+00	2026-10-06 21:05:31.137087+00	f
59	SS059	E2E Student 264741	2015-05-05	Grade 5	\N	\N	E2E Guardian 264741	905-555-0000	\N	e2e.parent.264741@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	18	\N	2026-10-06 21:05:33.576635+00	2026-10-06 21:05:38.708969+00	f
60	SS060	E2E Student 880580	2015-05-05	Grade 5	\N	\N	E2E Guardian 880580	905-555-0000	\N	e2e.parent.880580@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	18	\N	2026-10-06 21:05:33.587475+00	2026-10-06 21:05:38.73633+00	f
61	SS061	E2E Student 520209	2015-05-05	Grade 5	\N	\N	E2E Guardian 520209	905-555-0000	\N	e2e.parent.520209@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	18	\N	2026-10-06 21:05:33.597905+00	2026-10-06 21:05:38.752167+00	f
106	SS106	E2E Student 235293	2015-05-05	Grade 5	\N	\N	E2E Guardian 235293	905-555-0000	\N	e2e.parent.235293@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	\N	\N	2026-10-07 00:19:08.820523+00	2026-10-07 00:19:09.144053+00	f
107	SS107	Minimal Check	\N	Grade 2	\N	\N	Some Parent	\N	\N	\N	\N	\N	\N	15.00	ACTIVE	\N	\N	2026-10-07 00:19:16.972031+00	2026-10-07 00:19:16.972031+00	f
65	SS065	E2E Student 81618	2015-05-05	Grade 5	\N	\N	E2E Guardian 81618	905-555-0000	\N	e2e.parent.81618@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	\N	\N	2026-10-06 21:05:41.943119+00	2026-10-06 21:05:42.509802+00	f
66	SS066	E2E Student 706468	2015-05-05	Grade 5	\N	\N	E2E Guardian 706468	905-555-0000	\N	e2e.parent.706468@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	19	\N	2026-10-06 21:05:55.174136+00	2026-10-06 21:05:56.123462+00	f
67	SS067	E2E Student 459667	2015-05-05	Grade 5	\N	\N	E2E Guardian 459667	905-555-0000	\N	e2e.parent.459667@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	19	\N	2026-10-06 21:05:55.196647+00	2026-10-06 21:05:56.138371+00	f
68	SS068	E2E Student 416265	2015-05-05	Grade 5	\N	\N	E2E Guardian 416265	905-555-0000	\N	e2e.parent.416265@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	20	\N	2026-10-06 21:05:58.523371+00	2026-10-06 21:06:03.669295+00	f
69	SS069	E2E Student 879869	2015-05-05	Grade 5	\N	\N	E2E Guardian 879869	905-555-0000	\N	e2e.parent.879869@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	20	\N	2026-10-06 21:05:58.532811+00	2026-10-06 21:06:03.682848+00	f
70	SS070	E2E Student 350021	2015-05-05	Grade 5	\N	\N	E2E Guardian 350021	905-555-0000	\N	e2e.parent.350021@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	20	\N	2026-10-06 21:05:58.541641+00	2026-10-06 21:06:03.69666+00	f
75	SS075	E2E Student 931183	2015-05-05	Grade 5	\N	\N	E2E Guardian 931183	905-555-0000	\N	e2e.parent.931183@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	\N	\N	2026-10-06 21:06:17.806875+00	2026-10-06 21:06:18.141344+00	f
79	SS079	E2E Student 216383	2015-05-05	Grade 5	\N	\N	E2E Guardian 216383	905-555-0000	\N	e2e.parent.216383@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	22	\N	2026-10-06 21:06:47.983645+00	2026-10-06 21:06:53.30937+00	f
80	SS080	E2E Student 111388	2015-05-05	Grade 5	\N	\N	E2E Guardian 111388	905-555-0000	\N	e2e.parent.111388@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	22	\N	2026-10-06 21:06:47.993028+00	2026-10-06 21:06:53.3242+00	f
77	SS077	E2E Student 621443	2015-05-05	Grade 5	\N	\N	E2E Guardian 621443	905-555-0000	\N	e2e.parent.621443@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	21	\N	2026-10-06 21:06:44.01351+00	2026-10-06 21:06:44.930392+00	f
78	SS078	E2E Student 124005	2015-05-05	Grade 5	\N	\N	E2E Guardian 124005	905-555-0000	\N	e2e.parent.124005@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	21	\N	2026-10-06 21:06:44.026859+00	2026-10-06 21:06:44.943686+00	f
81	SS081	E2E Student 791256	2015-05-05	Grade 5	\N	\N	E2E Guardian 791256	905-555-0000	\N	e2e.parent.791256@example.com	E2E Emergency	905-555-0001	2026-01-05	20.00	INACTIVE	22	\N	2026-10-06 21:06:48.000599+00	2026-10-06 21:06:53.336218+00	f
\.


--
-- Data for Name: student_subject; Type: TABLE DATA; Schema: public; Owner: portal
--

COPY public.student_subject (student_id, subject) FROM stdin;
1	Science
1	Math
2	English
3	English
3	Math
4	Science
4	English
4	Math
5	Math
6	English
7	Math
8	English
9	Math
10	Math
11	Math
12	Math
13	Math
15	Math
16	Math
17	Math
18	Math
19	Math
20	Math
23	Math
24	Math
25	Math
26	Math
27	Math
28	Math
31	Math
32	Math
33	Math
34	Math
35	Math
36	Math
39	Math
40	Math
41	Math
42	Math
43	Math
44	Math
47	Math
48	Math
49	Math
50	Math
51	Math
52	Math
55	Math
57	Math
58	Math
59	Math
60	Math
61	Math
65	Math
66	Math
67	Math
68	Math
69	Math
70	Math
75	Math
77	Math
78	Math
79	Math
80	Math
81	Math
86	Math
87	Math
88	Math
89	Math
90	Math
91	Math
96	Math
97	Math
98	Math
99	Math
100	Math
101	Math
106	Math
\.


--
-- Data for Name: term; Type: TABLE DATA; Schema: public; Owner: portal
--

COPY public.term (id, name, start_date, end_date, calendar_name, active, created_at, updated_at) FROM stdin;
1	Fall 2026	2026-09-02	2026-12-19	Smart Start Classes	t	2026-10-06 03:53:56.743007+00	2026-10-06 03:53:56.743007+00
\.


--
-- Name: admin_user_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.admin_user_id_seq', 1, true);


--
-- Name: attendance_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.attendance_id_seq', 199, true);


--
-- Name: class_group_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.class_group_id_seq', 26, true);


--
-- Name: fee_payment_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.fee_payment_id_seq', 42, true);


--
-- Name: grade_rate_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.grade_rate_id_seq', 9, true);


--
-- Name: monthly_fee_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.monthly_fee_id_seq', 53, true);


--
-- Name: monthly_feedback_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.monthly_feedback_id_seq', 7, true);


--
-- Name: notification_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.notification_id_seq', 21, true);


--
-- Name: notification_template_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.notification_template_id_seq', 4, true);


--
-- Name: schedule_slot_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.schedule_slot_id_seq', 2, true);


--
-- Name: student_code_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.student_code_seq', 107, true);


--
-- Name: student_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.student_id_seq', 107, true);


--
-- Name: term_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.term_id_seq', 1, true);


--
-- Name: admin_user admin_user_pkey; Type: CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.admin_user
    ADD CONSTRAINT admin_user_pkey PRIMARY KEY (id);


--
-- Name: attendance attendance_pkey; Type: CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.attendance
    ADD CONSTRAINT attendance_pkey PRIMARY KEY (id);


--
-- Name: class_group class_group_pkey; Type: CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.class_group
    ADD CONSTRAINT class_group_pkey PRIMARY KEY (id);


--
-- Name: fee_payment fee_payment_pkey; Type: CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.fee_payment
    ADD CONSTRAINT fee_payment_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: grade_rate grade_rate_pkey; Type: CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.grade_rate
    ADD CONSTRAINT grade_rate_pkey PRIMARY KEY (id);


--
-- Name: monthly_fee monthly_fee_pkey; Type: CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.monthly_fee
    ADD CONSTRAINT monthly_fee_pkey PRIMARY KEY (id);


--
-- Name: monthly_feedback monthly_feedback_pkey; Type: CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.monthly_feedback
    ADD CONSTRAINT monthly_feedback_pkey PRIMARY KEY (id);


--
-- Name: notification notification_pkey; Type: CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT notification_pkey PRIMARY KEY (id);


--
-- Name: notification_template notification_template_pkey; Type: CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.notification_template
    ADD CONSTRAINT notification_template_pkey PRIMARY KEY (id);


--
-- Name: schedule_slot schedule_slot_pkey; Type: CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.schedule_slot
    ADD CONSTRAINT schedule_slot_pkey PRIMARY KEY (id);


--
-- Name: student student_pkey; Type: CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.student
    ADD CONSTRAINT student_pkey PRIMARY KEY (id);


--
-- Name: student_subject student_subject_pkey; Type: CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.student_subject
    ADD CONSTRAINT student_subject_pkey PRIMARY KEY (student_id, subject);


--
-- Name: term term_pkey; Type: CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.term
    ADD CONSTRAINT term_pkey PRIMARY KEY (id);


--
-- Name: attendance ux_attendance_student_date; Type: CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.attendance
    ADD CONSTRAINT ux_attendance_student_date UNIQUE (student_id, class_date);


--
-- Name: class_group ux_class_group_name; Type: CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.class_group
    ADD CONSTRAINT ux_class_group_name UNIQUE (name);


--
-- Name: monthly_feedback ux_feedback_student_month; Type: CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.monthly_feedback
    ADD CONSTRAINT ux_feedback_student_month UNIQUE (student_id, feedback_month);


--
-- Name: grade_rate ux_grade_rate_grade; Type: CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.grade_rate
    ADD CONSTRAINT ux_grade_rate_grade UNIQUE (grade);


--
-- Name: monthly_fee ux_monthly_fee_student_month; Type: CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.monthly_fee
    ADD CONSTRAINT ux_monthly_fee_student_month UNIQUE (student_id, fee_month);


--
-- Name: student ux_student_code; Type: CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.student
    ADD CONSTRAINT ux_student_code UNIQUE (student_code);


--
-- Name: notification_template ux_template_name; Type: CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.notification_template
    ADD CONSTRAINT ux_template_name UNIQUE (name);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: portal
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: ix_attendance_date; Type: INDEX; Schema: public; Owner: portal
--

CREATE INDEX ix_attendance_date ON public.attendance USING btree (class_date);


--
-- Name: ix_fee_payment_fee; Type: INDEX; Schema: public; Owner: portal
--

CREATE INDEX ix_fee_payment_fee ON public.fee_payment USING btree (monthly_fee_id);


--
-- Name: ix_feedback_month; Type: INDEX; Schema: public; Owner: portal
--

CREATE INDEX ix_feedback_month ON public.monthly_feedback USING btree (feedback_month);


--
-- Name: ix_monthly_fee_month; Type: INDEX; Schema: public; Owner: portal
--

CREATE INDEX ix_monthly_fee_month ON public.monthly_fee USING btree (fee_month);


--
-- Name: ix_notification_created; Type: INDEX; Schema: public; Owner: portal
--

CREATE INDEX ix_notification_created ON public.notification USING btree (created_at DESC);


--
-- Name: ix_notification_status; Type: INDEX; Schema: public; Owner: portal
--

CREATE INDEX ix_notification_status ON public.notification USING btree (status, scheduled_at);


--
-- Name: ix_slot_group; Type: INDEX; Schema: public; Owner: portal
--

CREATE INDEX ix_slot_group ON public.schedule_slot USING btree (class_group_id);


--
-- Name: ix_slot_term; Type: INDEX; Schema: public; Owner: portal
--

CREATE INDEX ix_slot_term ON public.schedule_slot USING btree (term_id);


--
-- Name: ix_student_full_name; Type: INDEX; Schema: public; Owner: portal
--

CREATE INDEX ix_student_full_name ON public.student USING btree (lower((full_name)::text));


--
-- Name: ix_student_grade; Type: INDEX; Schema: public; Owner: portal
--

CREATE INDEX ix_student_grade ON public.student USING btree (grade);


--
-- Name: ix_student_group; Type: INDEX; Schema: public; Owner: portal
--

CREATE INDEX ix_student_group ON public.student USING btree (class_group_id);


--
-- Name: ix_student_status; Type: INDEX; Schema: public; Owner: portal
--

CREATE INDEX ix_student_status ON public.student USING btree (status);


--
-- Name: ux_admin_user_username; Type: INDEX; Schema: public; Owner: portal
--

CREATE UNIQUE INDEX ux_admin_user_username ON public.admin_user USING btree (lower((username)::text));


--
-- Name: ux_term_single_active; Type: INDEX; Schema: public; Owner: portal
--

CREATE UNIQUE INDEX ux_term_single_active ON public.term USING btree (active) WHERE active;


--
-- Name: attendance attendance_student_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.attendance
    ADD CONSTRAINT attendance_student_id_fkey FOREIGN KEY (student_id) REFERENCES public.student(id);


--
-- Name: fee_payment fee_payment_monthly_fee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.fee_payment
    ADD CONSTRAINT fee_payment_monthly_fee_id_fkey FOREIGN KEY (monthly_fee_id) REFERENCES public.monthly_fee(id) ON DELETE CASCADE;


--
-- Name: monthly_fee monthly_fee_student_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.monthly_fee
    ADD CONSTRAINT monthly_fee_student_id_fkey FOREIGN KEY (student_id) REFERENCES public.student(id);


--
-- Name: monthly_feedback monthly_feedback_student_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.monthly_feedback
    ADD CONSTRAINT monthly_feedback_student_id_fkey FOREIGN KEY (student_id) REFERENCES public.student(id);


--
-- Name: notification notification_template_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT notification_template_id_fkey FOREIGN KEY (template_id) REFERENCES public.notification_template(id) ON DELETE SET NULL;


--
-- Name: schedule_slot schedule_slot_class_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.schedule_slot
    ADD CONSTRAINT schedule_slot_class_group_id_fkey FOREIGN KEY (class_group_id) REFERENCES public.class_group(id);


--
-- Name: schedule_slot schedule_slot_term_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.schedule_slot
    ADD CONSTRAINT schedule_slot_term_id_fkey FOREIGN KEY (term_id) REFERENCES public.term(id) ON DELETE CASCADE;


--
-- Name: student student_class_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.student
    ADD CONSTRAINT student_class_group_id_fkey FOREIGN KEY (class_group_id) REFERENCES public.class_group(id);


--
-- Name: student_subject student_subject_student_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: portal
--

ALTER TABLE ONLY public.student_subject
    ADD CONSTRAINT student_subject_student_id_fkey FOREIGN KEY (student_id) REFERENCES public.student(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict E8mARnGbhIi7WZfISOvMVF1YKcP3k9MvvozS64YYASadGEeJvvzxnonACJJdurc

