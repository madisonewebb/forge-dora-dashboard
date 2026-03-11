# Stage 1: build the React app
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY index.html tsconfig.json vite.config.ts ./
COPY src ./src
RUN npm run build

# Stage 2: serve with nginx
# nginx:alpine automatically runs envsubst on files in /etc/nginx/templates/
# at container startup, outputting to /etc/nginx/conf.d/ — so BACKEND_HOST
# is substituted at runtime rather than baked into the image.
FROM nginx:alpine
COPY nginx/nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
