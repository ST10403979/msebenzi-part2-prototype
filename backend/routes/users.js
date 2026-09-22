// routes/users.js
// Profile and settings endpoints — backs the app's Settings screen
// (change language, skills) and the Worker Profile & Verification feature.
const express = require("express");
const User = require("../models/User");
const { requireAuth } = require("../middleware/auth");

const router = express.Router();

// GET /api/users/me
router.get("/me", requireAuth, async (req, res) => {
  try {
    const user = await User.findByPk(req.user.id);
    if (!user) return res.status(404).json({ error: "User not found" });
    return res.json(publicUser(user));
  } catch (err) {
    console.error("[users] Fetch profile error:", err.message);
    return res.status(500).json({ error: "Server error" });
  }
});

// GET /api/users/:id — view another user's public profile (e.g. a
// household viewing a worker's profile before/after a job).
router.get("/:id", requireAuth, async (req, res) => {
  try {
    const user = await User.findByPk(req.params.id);
    if (!user) return res.status(404).json({ error: "User not found" });
    return res.json(publicUser(user));
  } catch (err) {
    console.error("[users] Fetch public profile error:", err.message);
    return res.status(500).json({ error: "Server error" });
  }
});

// PUT /api/users/me  — used by the Settings and Profile screens
router.put("/me", requireAuth, async (req, res) => {
  try {
    const { name, language, skills, bio } = req.body;
    const user = await User.findByPk(req.user.id);
    if (!user) return res.status(404).json({ error: "User not found" });

    if (name) user.name = name;
    if (language && ["en", "af", "xh"].includes(language)) user.language = language;
    if (typeof skills === "string") user.skills = skills;
    if (typeof bio === "string") {
      if (bio.length > 500) {
        return res.status(400).json({ error: "Bio must be 500 characters or fewer" });
      }
      user.bio = bio;
    }

    await user.save();
    console.log(`[users] Profile updated for ${user.email}`);

    return res.json({ message: "Profile updated", user: publicUser(user) });
  } catch (err) {
    console.error("[users] Update profile error:", err.message);
    return res.status(500).json({ error: "Server error" });
  }
});

// PUT /api/users/me/request-verification
// Feature: Worker profile and verification.
// A worker asks to be verified; status moves to "pending" until an
// admin approves it (see PUT /api/admin/users/:id/verify below).
router.put("/me/request-verification", requireAuth, async (req, res) => {
  try {
    const user = await User.findByPk(req.user.id);
    if (!user) return res.status(404).json({ error: "User not found" });
    if (user.role !== "worker") {
      return res.status(400).json({ error: "Only worker accounts can request verification" });
    }
    if (user.verificationStatus === "verified") {
      return res.status(409).json({ error: "This account is already verified" });
    }

    user.verificationStatus = "pending";
    await user.save();
    console.log(`[users] Verification requested by ${user.email}`);

    return res.json({ message: "Verification requested", user: publicUser(user) });
  } catch (err) {
    console.error("[users] Request-verification error:", err.message);
    return res.status(500).json({ error: "Server error" });
  }
});

function publicUser(user) {
  return {
    id: user.id,
    name: user.name,
    email: user.email,
    role: user.role,
    language: user.language,
    skills: user.skills,
    bio: user.bio,
    verificationStatus: user.verificationStatus,
    ratingAverage: user.ratingAverage,
    ratingCount: user.ratingCount,
  };
}

module.exports = router;
