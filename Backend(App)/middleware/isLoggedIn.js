// import { getTokenFromHeader } from "../utils/getTokenFromHeader.js";
// import { verifyToken } from "../utils/veriyToken.js";
// import User from "../model/user.js";

// export const isLoggedIn = async (req, res, next) => {
//   try {
//     const token = getTokenFromHeader(req);
//     const decodedUser = verifyToken(token);

//     if (!decodedUser) {
//       return res.status(401).json({ message: "Invalid or expired token" });
//     }

//     // Fetch the user from DB and attach to req
//     const user = await User.findById(decodedUser.id).select("-password");
//     if (!user) {
//       return res.status(401).json({ message: "User not found" });
//     }

//     req.user = user; // ✅ this enables req.user._id to work
//     next();
//   } catch (err) {
//     console.error("Auth error:", err);
//     return res.status(401).json({ message: "Not authorized" });
//   }
// };
import { getTokenFromHeader } from "../utils/getTokenFromHeader.js";
import { verifyToken } from "../utils/veriyToken.js"; // make sure this file & function name is correct
import User from "../model/user.js";

export const isLoggedIn = async (req, res, next) => {
  try {
    const token = getTokenFromHeader(req);

    if (!token) {
      return res.status(401).json({ message: "No token provided. Authorization denied." });
    }

    const decodedUser = verifyToken(token);

    if (!decodedUser) {
      return res.status(401).json({ message: "Invalid/Expired token" });
    }

    const user = await User.findById(decodedUser.id).select("-password"); // optional: exclude password
    if (!user) {
      return res.status(401).json({ message: "User not found" });
    }

    req.user = user; // attach full user to request
    req.userAuthId = user._id;
    next();
  } catch (error) {
    console.error("Auth Middleware Error:", error);
    return res.status(401).json({ message: "Not authorized" });
  }
};
