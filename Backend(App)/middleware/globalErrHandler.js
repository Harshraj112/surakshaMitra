export const globalErrHandler = (err, req, res, next) => {

  console.error(`[${req.method}] ${req.originalUrl} - ${err.name}: ${err.message}`);

  const statusCode = res.statusCode ? res.statusCode : 500; // Set default to 500 if not set

  res.status(statusCode);
  res.json({
    message: err.message,
    stack: process.env.NODE_ENV === "production" ? null : err.stack, // Hide stack trace in production
  });
}