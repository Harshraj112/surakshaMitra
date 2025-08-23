// routes/contactRoutes.js
import express from "express";
import { deleteEmergencyContact } from "../controllers/contactCtrl.js";
import { isLoggedIn } from "../middleware/isLoggedIn.js";

const router = express.Router();

router.delete("/contacts/:id", isLoggedIn, deleteEmergencyContact);

export default router;
