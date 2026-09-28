// Points the frontend at the backend API.
// To change the backend URL (e.g. different port, staging, docker-compose service name),
// edit `apiBaseUrl` here (or in environment.prod.ts for production builds).
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api/v1',
};
