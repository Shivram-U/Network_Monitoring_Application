# Use a Node.js image with version 18.19 or higher
FROM node:18.19 AS build

# Set the working directory
WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy the rest of the application code
COPY . .

# Build the Angular application
RUN npm run build --prod

# Debug: Check if dist directory exists
RUN ls -R /app/dist
