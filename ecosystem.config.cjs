module.exports = {
  apps: [
    {
      name: "customer",
      cwd: "./frontend/customer",
      script: "npm",
      args: "run dev",
      watch: false,
      autorestart: true,
      max_restarts: 5,
      env: {
        NODE_ENV: "development"
      }
    },
    {
      name: "backoffice",
      cwd: "./frontend/backoffice",
      script: "npm",
      args: "run dev",
      watch: false,
      autorestart: true,
      max_restarts: 5,
      env: {
        NODE_ENV: "development"
      }
    },
    {
      name: "admin",
      cwd: "./frontend/admin",
      script: "npm",
      args: "run dev",
      watch: false,
      autorestart: true,
      max_restarts: 5,
      env: {
        NODE_ENV: "development"
      }
    }
  ]
}
