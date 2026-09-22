// routes/auth.js
// Registration and login. Passwords are hashed with bcrypt (never stored
// or logged in plain text) and a JWT is issued on successful login.
const express = require("express");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const User = require("../models/User");

const router = express.Router();

const SALT_ROUNDS = 10;

// POST /api/auth/register
router.post("/register", async (req, res) => {
  try {
    const { name, email, password, role, language } = req.body;

    if (!name || !email || !password) {
      return res.status(400).json({ error: "name, email and password are required" });
    }
    if (password.length < 6) {
      return res.status(400).json({ error: "Password must be at least 6 characters" });
    }

    const existing = await User.findOne({ where: { email } });
    if (existing) {
      return res.status(409).json({ error: "An account with that email already exists" });
    }

    const passwordHash = await bcrypt.hash(password, SALT_ROUNDS);

    const user = await User.create({
      name,
      email,
      passwordHash,
      role: role === "household" ? "household" : "worker",
      language: language || "en",
    });

    console.log(`[auth] New user registered: ${user.email} (id=${user.id})`);

    return res.status(201).json({
      message: "Registration successful",
      user: publicUser(user),
    });
  } catch (err) {
    console.error("[auth] Register error:", err.message);
    return res.status(500).json({ error: "Server error during registration" });
  }
});

// POST /api/auth/login
router.post("/login", async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ error: "email and password are required" });
    }

    const user = await User.findOne({ where: { email } });
    if (!user) {
      return res.status(401).json({ error: "Invalid email or password" });
    }

    const match = await bcrypt.compare(password, user.passwordHash);
    if (!match) {
      return res.status(401).json({ error: "Invalid email or password" });
    }

    const token = jwt.sign(
      { id: user.id, email: user.email, role: user.role },
      process.env.JWT_SECRET || "dev_secret_change_me",
      { expiresIn: "7d" }
    );

    console.log(`[auth] User logged in: ${user.email}`);

    return res.json({
      message: "Login successful",
      token,
      user: publicUser(user),
    });
  } catch (err) {
    console.error("[auth] Login error:", err.message);
    return res.status(500).json({ error: "Server error during login" });
  }
});

// Strips the password hash and other internals before sending a user
// object back to the client.
function publicUser(user) {
  return {
    id: user.id,
    name: user.name,
    email: user.email,
    role: user.role,
    language: user.language,
    skills: user.skills,
    ratingAverage: user.ratingAverage,
    ratingCount: user.ratingCount,
  };
}

module.exports = router;
