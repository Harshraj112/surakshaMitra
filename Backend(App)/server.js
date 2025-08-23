import http from 'http';
import app from "./app/app.js";

//create the server
const PORT = process.env.PORT || 3000;
const server = http.createServer(app);
server.listen(PORT, console.log(`Server is up and running on port ${PORT}`));

process.on("unhandledRejection", (err) => {
  console.log("UNHANDLED REJECTION 🔥 Shutting down...");
  console.log(err.name, err.message);
  process.exit(1); // optional: allow Render to auto-restart
});
..