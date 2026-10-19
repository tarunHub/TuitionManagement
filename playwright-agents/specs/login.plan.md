# Login Test Plan

## Application Overview

Smart Start Student Portal sign-in page (`/login`). A single admin account signs in with a username and password;
on success the user lands on `/dashboard` (or on the protected page they originally requested). Client-side
validation requires both fields. Wrong credentials show "Invalid username or password". After
`LOGIN_MAX_FAILURES` (default 5) consecutive failures for a username, that username is locked for
`LOGIN_LOCKOUT` (default 15 minutes).

**Rules for every scenario**
- Negative scenarios must use a throwaway username (`throwawayUsername()` from `support/env.ts`), never the real admin,
  so the admin account is never locked.
- Valid credentials come from `env.username` / `env.password` (`support/env.ts`).

## Test Scenarios

### 1. Successful Login

**Seed:** `tests/seed.spec.ts`

#### 1.1 Login with valid credentials opens the dashboard

**File:** `tests/auth/login-with-valid-credentials.spec.ts`

**Steps:**
1. Open `/login`
   - expect: The sign-in form with Username, Password and "Sign in" button is visible
2. Enter the admin username and password and click "Sign in"
   - expect: The URL changes to `/dashboard`
   - expect: The "Dashboard" heading is visible
   - expect: The "Logout" button is visible

#### 1.2 Password visibility can be toggled

**File:** `tests/auth/password-visibility-toggle.spec.ts`

**Steps:**
1. Open `/login` and type any password
   - expect: The password field is masked (`type="password"`)
2. Click the "Show password" button
   - expect: The password field shows plain text (`type="text"`) and the button is labelled "Hide password"
3. Click the "Hide password" button
   - expect: The password field is masked again

#### 1.3 Login returns the user to the page they originally requested

**File:** `tests/auth/redirect-to-requested-page.spec.ts`

**Steps:**
1. While signed out, open `/students`
   - expect: The user is redirected to `/login`
2. Sign in with the admin credentials
   - expect: The URL changes to `/students`, not `/dashboard`

#### 1.4 Logout ends the session

**File:** `tests/auth/logout-ends-session.spec.ts`

**Steps:**
1. Sign in with the admin credentials
   - expect: The dashboard is shown
2. Click "Logout"
   - expect: The URL changes to `/login`
3. Open `/dashboard` directly
   - expect: The user is redirected to `/login`

### 2. Form Validation

**Seed:** `tests/seed.spec.ts`

#### 2.1 Submitting an empty form shows required-field errors

**File:** `tests/auth/empty-form-validation.spec.ts`

**Steps:**
1. Open `/login` and click "Sign in" without entering anything
   - expect: "Username is required" and "Password is required" are shown
   - expect: The URL stays on `/login`

#### 2.2 A whitespace-only username is rejected

**File:** `tests/auth/whitespace-username-validation.spec.ts`

**Steps:**
1. Open `/login`, enter three spaces as username and any password, click "Sign in"
   - expect: "Username is required" is shown
   - expect: No "Password is required" error is shown
   - expect: The URL stays on `/login`

### 3. Invalid Credentials

**Seed:** `tests/seed.spec.ts`

#### 3.1 Wrong credentials show an error

**File:** `tests/auth/invalid-credentials-error.spec.ts`

**Steps:**
1. Open `/login`, enter a throwaway username and a wrong password, click "Sign in"
   - expect: An alert with "Invalid username or password" is shown
   - expect: The URL stays on `/login`

#### 3.2 Repeated failures lock the username

**File:** `tests/auth/lockout-after-repeated-failures.spec.ts`

**Steps:**
1. Open `/login` and fail to sign in 5 times with the same throwaway username
   - expect: Each of the first 5 attempts shows "Invalid username or password"
2. Try a 6th time with the same username
   - expect: The alert says "Too many failed login attempts. Try again in N minute(s)."

### 4. Access Control

**Seed:** `tests/seed.spec.ts`

#### 4.1 Protected pages redirect signed-out users to login

**File:** `tests/auth/protected-route-redirects-to-login.spec.ts`

**Steps:**
1. While signed out, open `/dashboard`
   - expect: The URL changes to `/login` and the sign-in form is visible

#### 4.2 Signed-in users visiting /login are sent to the dashboard

**File:** `tests/auth/signed-in-user-skips-login.spec.ts`

**Steps:**
1. Sign in with the admin credentials
   - expect: The dashboard is shown
2. Open `/login` directly
   - expect: The URL changes to `/dashboard`
