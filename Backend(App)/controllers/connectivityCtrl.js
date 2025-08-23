// import User from "../model/user.js";

// export const updateConnectivityStatus = async (req, res) => {
//   const { userId, status } = req.body;

//   if (!userId || !status) {
//     return res.status(400).json({ message: "userId and status required" });
//   }

//   try {
//     const user = await User.findById(userId);
//     if (!user) return res.status(404).json({ message: "User not found" });

//     user.alerts.push({
//       triggeredAt: new Date(),
//       type: `connectivity-${status}`
//     });

//     await user.save();
//     res.json({ message: "Connectivity status updated" });
//   } catch (err) {
//     res.status(500).json({ message: err.message });
//   }
// };