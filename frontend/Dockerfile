# Basic multi-stage Dockerfile for the PaymentFlow Angular frontend.
# Phase 6 infra scaffolding — kept minimal/correct, not production-hardened.
#
# ASSUMPTION: frontend/ uses a standard Angular CLI project (package.json + angular.json
# at frontend/) and `ng build` (production config) emits output under
# frontend/dist/<project-name>/browser/ (Angular 17+ application builder layout).
# Adjust the COPY path below if the frontend agent's actual output differs.

# ---- Build stage ------------------------------------------------------------
FROM node:20-alpine AS build
WORKDIR /workspace

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration production

# ---- Runtime stage ------------------------------------------------------------
FROM nginx:1.27-alpine AS runtime

# Wildcard copies whichever project name `ng build` used, from the
# builder-application-format "browser" subfolder.
COPY --from=build /workspace/dist/*/browser /usr/share/nginx/html

# SPA fallback so Angular client-side routing works behind nginx.
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
