# project-ollivada
# 🛡️ ZeroPanic: Context-Aware Cyber Intelligence

![Status](https://img.shields.io/badge/Status-Concept%20%2F%20Prototype-orange)
![License](https://img.shields.io/badge/License-MIT-blue)
![AI-Powered](https://img.shields.io/badge/AI-Powered-green)

**ZeroPanic** is an AI-integrated security system designed to solve "alert fatigue" and reduce societal anxiety. Instead of broadcasting every global cyber threat to every user, it filters intelligence based on the user's specific digital footprint.

> **The Core Philosophy:** "Be aware of the issues you *need* to know, ignore the ones you don't."

---

## 🧐 The Problem
In the modern digital age, users are bombarded with news of data breaches, hacks, and ransomware attacks daily.
* **Information Overload:** Users cannot distinguish between a critical threat to their personal data and a hack on a service they have never used.
* **Societal Panic:** Constant bad news creates anxiety without providing a clear path to safety.
* **Alert Fatigue:** When everything is an emergency, users stop listening entirely.

## 💡 The Solution
**ZeroPanic** acts as a smart filter between global threat intelligence and the individual user.

1.  **User Profiling:** The system knows which apps, services, and devices you use (e.g., Instagram, Gmail, Chase Bank).
2.  **Global Monitoring:** It scans global cybersecurity news, CVE databases, and dark web leak reports.
3.  **Intelligent Matching:** The AI correlates the threat with your profile.
4.  **Selective Alerting:**
    * *Scenario A:* A massive leak hits **Instagram**. You have Instagram installed. **Action:** The system alerts you immediately with steps to change your password.
    * *Scenario B:* A massive leak hits **X.com (Twitter)**. You do not use X. **Action:** The system remains silent. You are safe.

---

## ⚙️ How It Works

### 1. Asset Discovery (The User Profile)
The user inputs their "digital stack" (or the app scans installed packages locally). This data is stored **locally and encrypted**—it is never uploaded to the cloud to ensure privacy.

* **Example Profile:** `['Instagram', 'WhatsApp', 'Wells Fargo', 'Windows 11']`

### 2. The Threat Engine
The backend aggregates data from:
* NIST NVD (National Vulnerability Database)
* Vendor Security Bulletins
* Tech News RSS Feeds
* "Have I Been Pwned" API

### 3. The Logic Layer (AI Filter)
The system uses NLP (Natural Language Processing) to parse threat reports and match them against the user profile.

```python
# Pseudocode Logic Example
def check_relevance(threat_report, user_apps):
    affected_service = extract_entity(threat_report) # AI extracts "Instagram" from news
    
    if affected_service in user_apps:
        priority = calculate_risk_score(threat_report)
        send_alert(priority, affected_service)
    else:
        log_event("Irrelevant threat suppressed to avoid panic.")
