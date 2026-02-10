<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'

const router = useRouter()

function goToLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="page">
    <div class="page__bg" />
    
    <div class="shell">
      <!-- Header -->
      <header class="head">
        <div class="head__left">
          <div class="head__brand">
            <Icon icon="iconoir:graduation-cap" class="head__logo" />
            <span class="head__title">EXAM.SYS</span>
          </div>
        </div>
        <div class="head__right">
          <el-button type="primary" round @click="goToLogin">
            登录系统
            <Icon icon="iconoir:arrow-right" style="margin-left: 4px" />
          </el-button>
        </div>
      </header>

      <!-- Hero Section -->
      <section class="hero">
        <div class="hero__content">
          <div class="hero__badge">ANTI-BORING EXAM SYSTEM</div>
          <h1 class="hero__title">
            重塑在线教育<br>
            <span class="highlight">每一个像素</span>
          </h1>
          <p class="hero__desc">
            告别千篇一律的考试系统。我们将美学与功能完美融合，为您带来前所未有的智能监控与数据洞察体验。
          </p>
          <div class="hero__actions">
            <el-button type="primary" size="large" round class="hero-btn" @click="goToLogin">
              立即开始体验
            </el-button>
            <el-button size="large" round class="hero-btn-outline" @click="goToLogin">
              查看演示
            </el-button>
          </div>
        </div>
        <div class="hero__visual">
          <div class="visual-card main-card">
            <div class="card-header">
              <div class="dots">
                <span class="dot red"></span>
                <span class="dot yellow"></span>
                <span class="dot green"></span>
              </div>
            </div>
            <div class="card-body">
              <div class="skeleton-line w-75"></div>
              <div class="skeleton-line w-50"></div>
              <div class="chart-placeholder">
                <div class="bar" style="height: 40%"></div>
                <div class="bar" style="height: 70%"></div>
                <div class="bar" style="height: 55%"></div>
                <div class="bar" style="height: 85%"></div>
              </div>
            </div>
          </div>
          <div class="visual-card float-card">
            <Icon icon="iconoir:check-circle" class="float-icon" />
            <div class="float-text">
              <span>智能批改</span>
              <small>准确率 99.9%</small>
            </div>
          </div>
        </div>
      </section>

      <!-- Features Section -->
      <section class="features">
        <div class="feature-card">
          <div class="icon-box">
            <Icon icon="iconoir:eye-alt" />
          </div>
          <h3>鹰眼监控</h3>
          <p>毫秒级切屏捕捉，全流程行为分析，让作弊行为无处遁形。</p>
        </div>
        
        <div class="feature-card">
          <div class="icon-box">
            <Icon icon="iconoir:stats-report" />
          </div>
          <h3>数据洞察</h3>
          <p>多维度的成绩分析与错题统计，精准定位知识盲区。</p>
        </div>

        <div class="feature-card">
          <div class="icon-box">
            <Icon icon="iconoir:flash" />
          </div>
          <h3>极速体验</h3>
          <p>基于最新技术栈构建，秒级响应，丝滑流畅的操作手感。</p>
        </div>

        <div class="feature-card">
          <div class="icon-box">
            <Icon icon="iconoir:design-pencil" />
          </div>
          <h3>精心设计</h3>
          <p>拒绝平庸的 B 端设计，每一个交互细节都经过精心打磨。</p>
        </div>
      </section>

      <!-- Footer -->
      <footer class="footer">
        <p>© 2026 EXAM.SYS. All rights reserved.</p>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  position: relative;
  background: #f6f8fa;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #1a1e23;
}

.page__bg {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 80% 10%, rgba(16, 212, 166, 0.08), transparent 40%),
    radial-gradient(circle at 10% 90%, rgba(255, 161, 22, 0.08), transparent 40%);
  pointer-events: none;
}

.shell {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  position: relative;
  z-index: 1;
}

/* Header */
.head {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.head__brand { display: flex; align-items: center; gap: 12px; color: #1a1e23; }
.head__logo { font-size: 32px; color: #10d4a6; }
.head__title { font-size: 20px; font-weight: 800; letter-spacing: -0.5px; }

/* Hero */
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 80px 0;
  gap: 60px;
}

.hero__content {
  flex: 1;
  max-width: 500px;
}

.hero__badge {
  display: inline-block;
  padding: 6px 12px;
  background: rgba(16, 212, 166, 0.1);
  color: #10d4a6;
  border-radius: 99px;
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 24px;
}

.hero__title {
  font-size: 56px;
  line-height: 1.1;
  font-weight: 900;
  margin-bottom: 24px;
  letter-spacing: -2px;
}

.highlight {
  color: #10d4a6;
  position: relative;
}

.highlight::after {
  content: '';
  position: absolute;
  bottom: 8px;
  left: 0;
  width: 100%;
  height: 12px;
  background: rgba(16, 212, 166, 0.2);
  z-index: -1;
  border-radius: 4px;
}

.hero__desc {
  font-size: 18px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 40px;
}

.hero__actions {
  display: flex;
  gap: 16px;
}

.hero-btn {
  padding: 24px 40px;
  font-size: 16px;
  font-weight: 600;
}

.hero__visual {
  flex: 1;
  position: relative;
  display: flex;
  justify-content: center;
}

/* Visual Cards */
.visual-card {
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.05);
}

.main-card {
  width: 400px;
  height: 300px;
  padding: 24px;
  position: relative;
  z-index: 1;
}

.card-header {
  margin-bottom: 24px;
}

.dots {
  display: flex;
  gap: 6px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.dot.red { background: #ff5f56; }
.dot.yellow { background: #ffbd2e; }
.dot.green { background: #27c93f; }

.skeleton-line {
  height: 12px;
  background: #f0f2f5;
  border-radius: 6px;
  margin-bottom: 12px;
}
.w-75 { width: 75%; }
.w-50 { width: 50%; }

.chart-placeholder {
  margin-top: 40px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 120px;
  padding: 0 20px;
}

.bar {
  width: 40px;
  background: #10d4a6;
  border-radius: 8px 8px 0 0;
  opacity: 0.8;
}
.bar:nth-child(2) { opacity: 0.4; }
.bar:nth-child(3) { opacity: 0.6; }

.float-card {
  position: absolute;
  bottom: -30px;
  left: 0;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  z-index: 2;
  animation: float 6s ease-in-out infinite;
}

.float-icon {
  font-size: 32px;
  color: #10d4a6;
}

.float-text {
  display: flex;
  flex-direction: column;
}
.float-text span { font-weight: 700; font-size: 14px; }
.float-text small { color: #999; font-size: 12px; }

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

/* Features */
.features {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  padding-bottom: 80px;
}

.feature-card {
  background: #fff;
  padding: 32px;
  border-radius: 20px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.02);
  transition: transform 0.3s;
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 32px rgba(0,0,0,0.05);
}

.icon-box {
  width: 48px;
  height: 48px;
  background: rgba(16, 212, 166, 0.1);
  color: #10d4a6;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-bottom: 20px;
}

.feature-card h3 {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 12px;
}

.feature-card p {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
}

/* Footer */
.footer {
  text-align: center;
  padding: 40px 0;
  border-top: 1px solid rgba(0,0,0,0.05);
  color: #999;
  font-size: 14px;
}

@media (max-width: 1000px) {
  .hero { flex-direction: column; text-align: center; padding: 40px 0; }
  .hero__content { margin: 0 auto; }
  .hero__actions { justify-content: center; }
  .features { grid-template-columns: repeat(2, 1fr); }
  .hero__visual { display: none; }
}
</style>