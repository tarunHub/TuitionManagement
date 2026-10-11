--
-- PostgreSQL database dump
--

\restrict 0Dc9aYM7JgWfHAzAWJopAp3oA0V1G47dF76hfQP6Cn26JlvrOhVqpxSYcO1JTI0

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
1	1	2026-08-02	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
2	1	2026-08-03	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
3	1	2026-08-05	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
4	1	2026-08-07	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
5	1	2026-08-10	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
6	1	2026-08-12	ABSENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
7	1	2026-08-14	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
8	1	2026-08-17	ABSENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
9	1	2026-08-19	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
10	1	2026-08-20	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
11	1	2026-08-24	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
12	1	2026-08-26	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
13	1	2026-08-31	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
14	12	2026-08-02	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
15	12	2026-08-04	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
16	12	2026-08-06	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
17	12	2026-08-08	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
18	12	2026-08-09	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
19	12	2026-08-11	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
20	12	2026-08-13	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
21	12	2026-08-14	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
22	12	2026-08-17	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
23	12	2026-08-18	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
24	12	2026-08-19	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
25	12	2026-08-20	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
26	12	2026-08-24	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
27	12	2026-08-25	ABSENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
28	12	2026-08-27	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
29	12	2026-08-31	ABSENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
30	2	2026-08-10	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
31	2	2026-08-11	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
32	2	2026-08-13	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
33	2	2026-08-14	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
34	2	2026-08-17	ABSENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
35	2	2026-08-18	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
36	2	2026-08-19	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
37	2	2026-08-20	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
38	2	2026-08-24	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
39	2	2026-08-25	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
40	2	2026-08-27	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
41	2	2026-08-31	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
42	3	2026-08-12	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
43	3	2026-08-14	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
44	3	2026-08-17	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
45	3	2026-08-19	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
46	3	2026-08-20	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
47	3	2026-08-24	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
48	3	2026-08-26	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
49	3	2026-08-31	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
50	4	2026-08-13	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
51	4	2026-08-17	ABSENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
52	4	2026-08-20	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
53	4	2026-08-24	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
54	4	2026-08-27	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
55	4	2026-08-31	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
56	5	2026-08-17	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
57	5	2026-08-18	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
58	5	2026-08-19	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
59	5	2026-08-20	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
60	5	2026-08-24	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
61	5	2026-08-25	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
62	5	2026-08-27	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
63	5	2026-08-31	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
64	1	2026-09-01	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
65	1	2026-09-03	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
66	1	2026-09-07	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
67	1	2026-09-09	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
68	1	2026-09-10	ABSENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
69	1	2026-09-11	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
70	1	2026-09-14	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
71	1	2026-09-16	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
72	1	2026-09-17	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
73	1	2026-09-18	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
74	1	2026-09-21	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
75	1	2026-09-23	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
76	1	2026-09-24	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
77	2	2026-09-01	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
78	2	2026-09-03	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
79	2	2026-09-04	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
80	2	2026-09-07	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
81	2	2026-09-08	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
82	2	2026-09-10	ABSENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
83	2	2026-09-14	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
84	2	2026-09-15	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
85	2	2026-09-17	ABSENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
86	2	2026-09-21	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
87	2	2026-09-22	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
88	2	2026-09-24	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
89	3	2026-09-01	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
90	3	2026-09-03	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
91	3	2026-09-07	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
92	3	2026-09-09	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
93	3	2026-09-10	ABSENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
94	3	2026-09-11	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
95	3	2026-09-14	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
96	3	2026-09-16	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
97	3	2026-09-17	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
98	3	2026-09-18	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
99	3	2026-09-21	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
100	3	2026-09-23	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
101	3	2026-09-24	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
102	4	2026-09-03	ABSENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
103	4	2026-09-07	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
104	4	2026-09-10	ABSENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
105	4	2026-09-14	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
106	4	2026-09-17	ABSENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
107	4	2026-09-21	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
108	4	2026-09-24	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
109	5	2026-09-01	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
110	5	2026-09-03	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
111	5	2026-09-04	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
112	5	2026-09-07	ABSENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
113	5	2026-09-09	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
114	5	2026-09-11	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
115	5	2026-09-14	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
116	5	2026-09-16	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
117	5	2026-09-18	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
118	5	2026-09-21	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
119	5	2026-09-23	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
120	5	2026-09-24	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
121	6	2026-09-08	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
122	6	2026-09-10	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
123	6	2026-09-14	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
124	6	2026-09-15	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
125	6	2026-09-17	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
126	6	2026-09-21	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
127	6	2026-09-22	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
128	6	2026-09-24	ABSENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
129	7	2026-09-08	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
130	7	2026-09-10	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
131	7	2026-09-14	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
132	7	2026-09-15	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
133	7	2026-09-17	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
134	7	2026-09-21	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
135	7	2026-09-22	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
136	7	2026-09-24	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
137	8	2026-09-06	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
138	8	2026-09-15	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
139	8	2026-09-23	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
140	10	2026-09-06	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
141	10	2026-09-15	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
142	10	2026-09-23	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
143	1	2026-10-01	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
144	1	2026-10-02	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
145	1	2026-10-05	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
146	2	2026-10-01	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
147	2	2026-10-05	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
148	2	2026-10-06	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
149	3	2026-10-01	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
150	3	2026-10-02	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
151	3	2026-10-05	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
152	4	2026-10-01	ABSENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
153	5	2026-10-01	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
154	5	2026-10-02	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
155	5	2026-10-05	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
156	6	2026-10-01	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
157	6	2026-10-05	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
158	6	2026-10-06	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
159	7	2026-10-01	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
160	7	2026-10-05	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
161	7	2026-10-06	PRESENT	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
\.


--
-- Data for Name: class_group; Type: TABLE DATA; Schema: public; Owner: portal
--

COPY public.class_group (id, name, description, created_at, updated_at) FROM stdin;
1	Batch1	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
2	Batch2	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
3	Batch3	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
4	Batch4	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
\.


--
-- Data for Name: fee_payment; Type: TABLE DATA; Schema: public; Owner: portal
--

COPY public.fee_payment (id, monthly_fee_id, amount, payment_date, method, reference, created_at, updated_at) FROM stdin;
1	1	260.00	2026-08-28	CASH	Excel import	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
2	2	160.00	2026-08-02	CASH	Excel import	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
3	3	180.00	2026-08-18	CASH	Excel import	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
4	4	160.00	2026-08-17	CASH	Excel import	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
5	5	100.00	2026-08-20	CASH	Excel import	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
6	6	100.00	2026-08-31	CASH	Excel import	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
7	7	260.00	2026-09-30	OTHER	Excel import; date not recorded; method not recorded	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
8	8	180.00	2026-09-30	OTHER	Excel import; date not recorded; method not recorded	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
9	9	260.00	2026-09-30	OTHER	Excel import; date not recorded; method not recorded	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
10	10	110.00	2026-09-30	OTHER	Excel import; date not recorded; method not recorded	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
11	11	120.00	2026-09-30	OTHER	Excel import; date not recorded; method not recorded	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
12	12	120.00	2026-09-30	OTHER	Excel import; date not recorded; method not recorded	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
13	13	160.00	2026-09-30	OTHER	Excel import; date not recorded; method not recorded	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
14	15	75.00	2026-09-30	OTHER	Excel import; date not recorded; method not recorded	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
15	18	80.00	2026-10-05	CASH	Excel import	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
16	20	5.00	2026-10-06	OTHER	Excel import; date not recorded; method not recorded	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
17	22	195.00	2026-10-06	CASH	Excel import	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
18	23	260.00	2026-10-06	CASH	Excel import	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
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
1	JK	10.00	Junior Kindergarten	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
2	SK	10.00	Senior Kindergarten	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
3	Grade 1	15.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
4	Grade 2	15.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
5	Grade 3	15.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
6	Grade 4	15.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
7	Grade 5	20.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
8	Grade 6	20.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
9	Coding	25.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
10	Grade 7	20.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
11	Grade 8	20.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
\.


--
-- Data for Name: monthly_fee; Type: TABLE DATA; Schema: public; Owner: portal
--

COPY public.monthly_fee (id, student_id, fee_month, rate, classes_held, classes_scheduled, scheduled_override, amount_due, notes, created_at, updated_at) FROM stdin;
1	1	2026-08-01	20.00	13	13	t	260.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
2	12	2026-08-01	10.00	16	16	t	160.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
3	2	2026-08-01	15.00	12	12	t	180.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
4	3	2026-08-01	20.00	8	8	t	160.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
5	4	2026-08-01	15.00	6	6	t	90.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
6	5	2026-08-01	10.00	8	8	t	80.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
7	1	2026-09-01	20.00	13	13	t	260.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
8	2	2026-09-01	15.00	12	12	t	180.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
9	3	2026-09-01	20.00	13	13	t	260.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
10	4	2026-09-01	15.00	7	7	t	105.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
11	5	2026-09-01	10.00	12	12	t	120.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
12	6	2026-09-01	15.00	8	8	t	120.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
13	7	2026-09-01	20.00	8	8	t	160.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
14	8	2026-09-01	25.00	3	3	t	75.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
15	10	2026-09-01	25.00	3	3	t	75.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
16	11	2026-09-01	25.00	0	0	t	0.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
17	1	2026-10-01	20.00	3	16	t	60.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
18	2	2026-10-01	15.00	3	13	t	45.00	Parially paid	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
19	3	2026-10-01	20.00	3	16	t	60.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
20	4	2026-10-01	15.00	1	6	t	15.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
21	5	2026-10-01	10.00	3	14	t	30.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
22	6	2026-10-01	15.00	3	13	t	45.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
23	7	2026-10-01	20.00	3	13	t	60.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
24	8	2026-10-01	25.00	0	4	t	0.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
25	9	2026-10-01	15.00	0	6	t	0.00	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
\.


--
-- Data for Name: monthly_feedback; Type: TABLE DATA; Schema: public; Owner: portal
--

COPY public.monthly_feedback (id, student_id, feedback_month, topics_taught, improvement_areas, achievements, created_at, updated_at) FROM stdin;
1	1	2026-08-01	LCM GCF, Ecosystem, world map, Exponents, Nouns, Psuedo coding	\N	Good learner	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
2	12	2026-08-01	Letters A-E, number 1-10, counting number 1-10, simple addition technique on fingers	word hard on learning letters and phonics	Going slow in english but picking maths well	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
3	2	2026-08-01	Maths operations add, add with regrouping, subtract, 3 letter words, 4 letter s blends	Lot of reading practice needed	Good progress in class	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
4	3	2026-08-01	LCM GCF, Ecosystem, world map,Psuedo coding	Need a lot of pratice in maths tables multiplication and division	Good progress in class	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
5	4	2026-08-01	Maths operations add, add with regrouping, subtract, 3 letter words, 4 letter s blends	More focus on words and spelling	Picking maths operation at her own pace	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
6	5	2026-08-01	Letters A-z capital, small a-d. counting 1-10, simple addition technique on fingers	Need to learn phonics	Good at maths	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
7	1	2026-09-01	Food chain, food web, enerygy pyramid, carbon and water cycle, Decimals, exponents, power of tens, Nouns, plate techtonics, landforms,Squares and square roots, verbs, tenses (past)	\N	Good learner	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
8	2	2026-09-01	Addition of 2 digit numbers with word problems, subtraction of 2 digit number (no regrouping) with problems, 4 letter words r blends and l blends, nasal blends "mp". Animal classifications and characteristics, cursive writing a-o	Focus more on reading level 1 books	Good progress in class	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
9	3	2026-09-01	Food chain, food web, enerygy pyramid, carbon and water cycle, Decimals, exponents, power of tens, Nouns, plate techtonics, landforms,Squares and square roots, verbs, tenses (past)	Need a lot of pratice in maths tables multiplication and division	Good progress in class	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
10	4	2026-09-01	Addition of 2 digit numbers with word problems, subtraction of 2 digit number (no regrouping) with problems, 4 letter words r blends and l blends, reading comprehension, nasal blends "mp".	Need writing practice	Good progress in class. Try to attend all classes	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
11	5	2026-09-01	small e-z, letter at and words with it. adding numbers 1-10 with simple addition technique on fingers,2 letter words at, in, on, 3 letter words with "AT" and associated 3 letter words.	Need to learn phonics	Good progress in class	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
12	6	2026-09-01	Addition of 2 digit number, word problems, round ups to tens, subtraction of 2 digit numbers, round ups to 100, reading comprehension, word problems on addition and subtraction, nasal blends "mp".	Focus on communication more at home	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
13	7	2026-09-01	Round up to 10,000. Subtraction of 4 digit numbers, common noun vs proper nouns vs compound nouns, transitive and intransitive verbs, reading comprehension, multiplication 2 digit numbers,money addition subraction, matter, states of matter, properties of matter, units of matter	Focus on communication more at home	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
14	1	2026-10-01	Patterns, Multiplication and Division	\N	Good learner	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
15	2	2026-10-01	BL cl words reading comprehension, "at" "an" "ap" "ad" "am" "ag" words along with short stories	\N	Good progress in class	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
16	3	2026-10-01	Patterns, Multiplication and Division	\N	Good progress in class	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
17	4	2026-10-01		\N	Good progress in class. Try to attend all classes	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
18	5	2026-10-01	AT, IN words	\N	Good progress in class	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
19	6	2026-10-01	BL cl words reading comprehension, 2 digit addition	\N	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
20	7	2026-10-01	Cells, Decimals, 3 digit division worksheet	\N	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
\.


--
-- Data for Name: notification; Type: TABLE DATA; Schema: public; Owner: portal
--

COPY public.notification (id, type, channel, subject, body, recipients, status, scheduled_at, sent_at, error_message, template_id, created_at, updated_at) FROM stdin;
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
1	1	1	WED	17:00:00	18:00:00	\N	\N	\N	\N	Weekly	\N	2026-10-07 01:52:19.582113+00	2026-10-07 01:52:19.582113+00
\.


--
-- Data for Name: student; Type: TABLE DATA; Schema: public; Owner: portal
--

COPY public.student (id, student_code, full_name, date_of_birth, grade, school, address, guardian_name, primary_contact, alternate_contact, email, emergency_contact_name, emergency_contact_phone, enrollment_date, per_class_rate, status, class_group_id, notes, created_at, updated_at, sms_opt_in) FROM stdin;
1	SS001	Fateh	\N	Grade 7	Tony Pontes Public School	62 Blackberry valley Crescent, Caledon, ON	Jasbir	416-893-0898	\N	\N	\N	\N	\N	20.00	ACTIVE	1	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00	f
2	SS002	Ravjodh	\N	Grade 2	Tony Pontes Public School	158 Newhouse Boulevard	Maninder	647-201-6812	\N	\N	\N	\N	\N	15.00	ACTIVE	3	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00	f
3	SS003	Alaina	2014-10-08	Grade 7	Tony Pontes Public School	156 Newhouse boulevard	Priya Tandon	226-886-4462	\N	\N	\N	\N	\N	20.00	ACTIVE	1	Want to pair the same day as Ravjodh	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00	f
4	SS004	Kaya	\N	Grade 3	Tony Pontes Public School	\N	Bina	647-708-8550	\N	\N	\N	\N	\N	15.00	ACTIVE	3	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00	f
5	SS005	Gurshabd Singh Rawala	2021-03-29	SK	Tony Pontes Public School	48 Blackberry Valley Crescent Caledon	Ashwinder	647-939-6794	437-430-6111	\N	\N	\N	\N	10.00	ACTIVE	2	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00	f
6	SS006	Baani Purba	2022-07-25	Grade 3	James Grieve Public School	27 Valleyscape Trail, Caledon	Gagandeep Purba	647-260-6420	647-648-5718	\N	\N	\N	\N	15.00	ACTIVE	3	Planned from sept 8	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00	f
7	SS007	Avneet Kaur Grewal	2016-07-16	Grade 5	James Grieve Public School	25 Valleyscape Trail, Caledon	Jagdip Singh Grewal	416-844-6548	647-389-6783	\N	\N	\N	\N	20.00	ACTIVE	3	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00	f
8	SS008	Tanisha	\N	Coding	Mayfield secondary school	\N	Ruchi	\N	\N	\N	\N	\N	\N	25.00	ACTIVE	4	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00	f
9	SS009	xyz	\N	Grade 2	Somerset Public school	\N	Sharn	\N	\N	\N	\N	\N	\N	15.00	INACTIVE	\N	Status in spreadsheet: Inquiry	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00	f
10	SS010	Ekamjot	\N	Coding	Mayfield secondary school	\N	Jasbir	\N	\N	\N	\N	\N	\N	25.00	INACTIVE	4	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00	f
11	SS011	Viraj	\N	Coding	Mayfield secondary school	\N	Sonia	\N	\N	\N	\N	\N	\N	25.00	INACTIVE	4	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00	f
12	SS012	Amolpreet	\N	SK	Tony Pontes Public School	560 Dougall Avenue, Caledon	Khushi	437-979-7072	\N	\N	\N	\N	\N	10.00	INACTIVE	2	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00	f
13	SS013	Ekjot	\N	Grade 1	Tony Pontes Public School	\N	Aman	647-473-2683	\N	\N	\N	\N	\N	15.00	INACTIVE	\N	Want classes on weekend atleast1	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00	f
14	SS014	Aarna	\N	JK	Tony Pontes Public School	\N	Gopinath	289-207-2439	\N	\N	\N	\N	\N	10.00	INACTIVE	\N	\N	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00	f
\.


--
-- Data for Name: student_subject; Type: TABLE DATA; Schema: public; Owner: portal
--

COPY public.student_subject (student_id, subject) FROM stdin;
1	All
2	All
3	All
4	All
5	All
6	All
7	All
8	Coding
9	All
10	Coding
11	Coding
12	All
13	All
14	All
\.


--
-- Data for Name: term; Type: TABLE DATA; Schema: public; Owner: portal
--

COPY public.term (id, name, start_date, end_date, calendar_name, active, created_at, updated_at) FROM stdin;
1	Fall 2026	2026-09-02	2026-12-19	Smart Start Classes	t	2026-10-07 00:21:17.302882+00	2026-10-07 00:21:17.302882+00
\.


--
-- Name: admin_user_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.admin_user_id_seq', 1, true);


--
-- Name: attendance_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.attendance_id_seq', 161, true);


--
-- Name: class_group_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.class_group_id_seq', 4, true);


--
-- Name: fee_payment_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.fee_payment_id_seq', 18, true);


--
-- Name: grade_rate_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.grade_rate_id_seq', 11, true);


--
-- Name: monthly_fee_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.monthly_fee_id_seq', 25, true);


--
-- Name: monthly_feedback_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.monthly_feedback_id_seq', 20, true);


--
-- Name: notification_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.notification_id_seq', 1, false);


--
-- Name: notification_template_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.notification_template_id_seq', 4, true);


--
-- Name: schedule_slot_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.schedule_slot_id_seq', 1, true);


--
-- Name: student_code_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.student_code_seq', 14, true);


--
-- Name: student_id_seq; Type: SEQUENCE SET; Schema: public; Owner: portal
--

SELECT pg_catalog.setval('public.student_id_seq', 14, true);


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

\unrestrict 0Dc9aYM7JgWfHAzAWJopAp3oA0V1G47dF76hfQP6Cn26JlvrOhVqpxSYcO1JTI0

