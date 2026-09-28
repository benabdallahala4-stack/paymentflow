export type Role = 'CUSTOMER' | 'ADMIN';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
}

// api-design.md documents `Authorization: Bearer <JWT>` but does not finalize the
// exact login response body. We assume a standard access/refresh token pair plus
// a minimal user summary, consistent with ADR-007 (JWT bearer auth).
export interface AuthResponse {
  accessToken: string;
  refreshToken?: string;
  userId: string;
  email: string;
  role: Role;
}

export interface RefreshRequest {
  refreshToken: string;
}
