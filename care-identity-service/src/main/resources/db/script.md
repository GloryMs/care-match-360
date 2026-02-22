3.1 Create Migration Scripts
Directory: care-identity-service/src/main/resources/db/migration/
File: V1__create_users_table.sql
sql-- Create users table
CREATE TABLE IF NOT EXISTS care_identity.users (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
email VARCHAR(255) NOT NULL UNIQUE,
password_hash VARCHAR(255) NOT NULL,
role VARCHAR(50) NOT NULL CHECK (role IN ('PATIENT', 'RELATIVE', 'RESIDENTIAL_PROVIDER', 'AMBULATORY_PROVIDER', 'ADMIN', 'SUPER_ADMIN')),
is_verified BOOLEAN DEFAULT FALSE,
is_active BOOLEAN DEFAULT TRUE,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on email for faster lookups
CREATE INDEX idx_users_email ON care_identity.users(email);
CREATE INDEX idx_users_role ON care_identity.users(role);

File: V2__create_email_verification_tokens_table.sql
sql-- Create email verification tokens table
CREATE TABLE IF NOT EXISTS care_identity.email_verification_tokens (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
user_id UUID NOT NULL REFERENCES care_identity.users(id) ON DELETE CASCADE,
token VARCHAR(255) NOT NULL UNIQUE,
expires_at TIMESTAMP NOT NULL,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
used_at TIMESTAMP
);

-- Create index for faster token lookups
CREATE INDEX idx_verification_tokens_token ON care_identity.email_verification_tokens(token);
CREATE INDEX idx_verification_tokens_user_id ON care_identity.email_verification_tokens(user_id);

File: V3__create_password_reset_tokens_table.sql
sql-- Create password reset tokens table
CREATE TABLE IF NOT EXISTS care_identity.password_reset_tokens (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
user_id UUID NOT NULL REFERENCES care_identity.users(id) ON DELETE CASCADE,
token VARCHAR(255) NOT NULL UNIQUE,
expires_at TIMESTAMP NOT NULL,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
used_at TIMESTAMP
);

-- Create index for faster token lookups
CREATE INDEX idx_reset_tokens_token ON care_identity.password_reset_tokens(token);
CREATE INDEX idx_reset_tokens_user_id ON care_identity.password_reset_tokens(user_id);

File: V4__create_two_factor_auth_table.sql
sql-- Create two-factor authentication table
CREATE TABLE IF NOT EXISTS care_identity.two_factor_auth (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
user_id UUID NOT NULL UNIQUE REFERENCES care_identity.users(id) ON DELETE CASCADE,
secret VARCHAR(255) NOT NULL,
is_enabled BOOLEAN DEFAULT FALSE,
backup_codes TEXT[], -- Array of backup codes
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on user_id
CREATE INDEX idx_two_factor_user_id ON care_identity.two_factor_auth(user_id);

File: V5__create_refresh_tokens_table.sql
sql-- Create refresh tokens table (for token management)
CREATE TABLE IF NOT EXISTS care_identity.refresh_tokens (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
user_id UUID NOT NULL REFERENCES care_identity.users(id) ON DELETE CASCADE,
token VARCHAR(500) NOT NULL UNIQUE,
expires_at TIMESTAMP NOT NULL,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
revoked_at TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_refresh_tokens_token ON care_identity.refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON care_identity.refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON care_identity.refresh_tokens(expires_at);
