<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { http } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { Icon } from '@iconify/vue'

type CaptchaResp = { captchaId: string; imageBase64: string }

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const activeTab = ref<'login' | 'register'>('login')
const captcha = ref<CaptchaResp | null>(null)

const loginFormRef = ref<FormInstance>()
const loginForm = reactive({
  username: '',
  password: '',
  captchaCode: '',
})

const loginRules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

const registerFormRef = ref<FormInstance>()
const registerForm = reactive({
  username: '',
  realName: '',
  password: '',
  confirmPassword: '',
})

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入账号/学号', trigger: 'blur' },
    { min: 4, max: 64, message: '长度 4-64', trigger: 'blur' },
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '长度 6-32', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== registerForm.password) callback(new Error('两次密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

async function refreshCaptcha() {
  try {
    const res = await http.get('/api/auth/captcha')
    captcha.value = res.data.data as CaptchaResp
  } catch (e: any) {
    ElMessage.error(e?.message || '验证码获取失败')
  }
}

async function submitLogin() {
  if (!captcha.value) {
    await refreshCaptcha()
    return
  }
  const ok = await loginFormRef.value?.validate().catch(() => false)
  if (!ok) return

  loading.value = true
  try {
    const res = await http.post('/api/auth/login', {
      username: loginForm.username,
      password: loginForm.password,
      captchaId: captcha.value.captchaId,
      captchaCode: loginForm.captchaCode,
    })
    const data = res.data.data as { accessToken: string; refreshToken: string }
    auth.setTokens(data.accessToken, data.refreshToken)
    await auth.fetchMe()
    await router.replace('/dashboard')
  } catch (e: any) {
    ElMessage.error(e?.message || '登录失败')
    loginForm.captchaCode = ''
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}

async function submitRegister() {
  const ok = await registerFormRef.value?.validate().catch(() => false)
  if (!ok) return

  loading.value = true
  try {
    await http.post('/api/auth/register', {
      username: registerForm.username,
      realName: registerForm.realName,
      password: registerForm.password,
    })
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.username = registerForm.username
    loginForm.password = ''
    loginForm.captchaCode = ''
    await refreshCaptcha()
  } catch (e: any) {
    ElMessage.error(e?.message || '注册失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  if (auth.accessToken) {
    try {
      await auth.fetchMe()
      await router.replace('/dashboard')
      return
    } catch {
      auth.logout()
    }
  }
  await refreshCaptcha()
})
</script>

<template>
  <div class="page">
    <!-- Left Side: Intro -->
    <div class="intro-side">
      <div class="intro-bg"></div>
      <div class="intro-content fade-in">
        <div class="brand">
          <div class="logo-box">
            <Icon icon="iconoir:graduation-cap" class="logo-icon-lg" />
          </div>
          <span class="brand-text">ExamSystem</span>
        </div>
        
        <h1 class="main-title">
          <span class="highlight">开启未来教育</span>
        </h1>
        
        <p class="sub-desc">
          新一代在线考试管理平台，集智能组卷、实时监控、多维分析于一体。让考核更公平，让管理更高效。
        </p>
        
        <div class="features">
          <div class="feature-item">
            <div class="feature-icon">
              <Icon icon="iconoir:magic-wand" />
            </div>
            <div class="feature-text">
              <h3>智能组卷</h3>
              <p>遗传算法自动生成最优试卷</p>
            </div>
          </div>
          
          <div class="feature-item">
            <div class="feature-icon">
              <Icon icon="iconoir:stats-report" />
            </div>
            <div class="feature-text">
              <h3>多维分析</h3>
              <p>精准把控教学质量与进度</p>
            </div>
          </div>
          
          <div class="feature-item">
            <div class="feature-icon">
              <Icon icon="iconoir:shield-check" />
            </div>
            <div class="feature-text">
              <h3>安全稳定</h3>
              <p>全方位防作弊与数据保护</p>
            </div>
          </div>
        </div>
        

      </div>
      
      <!-- Decorative Elements for Left Side -->
      <div class="decorative-circle circle-1"></div>
      <div class="decorative-circle circle-2"></div>
      <div class="decorative-shape shape-1"></div>
    </div>

    <!-- Right Side: Form -->
    <div class="form-side">
      <div class="page__bg" /> <!-- Background for right side -->
      
      <div class="layout fade-in-right">
        <div class="panel">
          <div class="panel__header">
            <!-- Mobile Logo (visible only on small screens) -->
            <div class="mobile-logo">
              <Icon icon="iconoir:graduation-cap" class="logo-icon" />
            </div>
            <h1 class="welcome-title">欢迎回来</h1>
            <p class="welcome-desc">请登录您的账号以继续</p>
          </div>
          
          <el-card class="panel__card" shadow="hover">
            <el-tabs v-model="activeTab" class="panel__tabs" stretch>
              <el-tab-pane label="登录" name="login">
                <div class="tab-content slide-up">
                  <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top" size="large">
                    <el-form-item label="账号" prop="username">
                      <el-input v-model="loginForm.username" placeholder="请输入学号 / 工号 / 账号" autocomplete="username">
                        <template #prefix>
                          <Icon icon="iconoir:user" class="input-icon" />
                        </template>
                      </el-input>
                    </el-form-item>

                    <el-form-item label="密码" prop="password">
                      <el-input
                        v-model="loginForm.password"
                        placeholder="请输入密码"
                        autocomplete="current-password"
                        show-password
                        type="password"
                        @keyup.enter="submitLogin"
                      >
                        <template #prefix>
                          <Icon icon="iconoir:key" class="input-icon" />
                        </template>
                      </el-input>
                    </el-form-item>

                    <el-form-item label="验证码" prop="captchaCode">
                      <div class="captcha-row">
                        <el-input v-model="loginForm.captchaCode" placeholder="输入验证码" @keyup.enter="submitLogin">
                          <template #prefix>
                            <Icon icon="iconoir:security-pass" class="input-icon" />
                          </template>
                        </el-input>
                        <div class="captcha-img-wrapper" @click="refreshCaptcha">
                          <el-image
                            v-if="captcha"
                            class="captcha__img"
                            :src="captcha.imageBase64"
                            fit="contain"
                          />
                          <el-skeleton v-else class="captcha__img" animated />
                        </div>
                      </div>
                    </el-form-item>

                    <el-form-item>
                      <el-button :loading="loading" class="panel__primary" type="primary" @click="submitLogin" round>
                        登录
                        <Icon icon="iconoir:arrow-right" class="btn-icon" />
                      </el-button>
                    </el-form-item>
                  </el-form>
                </div>
              </el-tab-pane>

              <el-tab-pane label="注册" name="register">
                <div class="tab-content slide-up">
                  <el-alert :closable="false" show-icon title="仅限学生自行注册，教师请联系管理员" type="info" class="mb-4" />
                  <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top" size="large">
                    <el-form-item label="学号" prop="username">
                      <el-input v-model="registerForm.username" placeholder="请输入学号作为账号" autocomplete="username">
                        <template #prefix>
                          <Icon icon="iconoir:id-card" class="input-icon" />
                        </template>
                      </el-input>
                    </el-form-item>

                    <el-form-item label="姓名" prop="realName">
                      <el-input v-model="registerForm.realName" placeholder="请输入真实姓名" autocomplete="name">
                        <template #prefix>
                          <Icon icon="iconoir:profile-circle" class="input-icon" />
                        </template>
                      </el-input>
                    </el-form-item>

                    <el-form-item label="密码" prop="password">
                      <el-input
                        v-model="registerForm.password"
                        placeholder="设置您的密码"
                        show-password
                        type="password"
                        autocomplete="new-password"
                      >
                        <template #prefix>
                          <Icon icon="iconoir:lock" class="input-icon" />
                        </template>
                      </el-input>
                    </el-form-item>

                    <el-form-item label="确认密码" prop="confirmPassword">
                      <el-input
                        v-model="registerForm.confirmPassword"
                        placeholder="再次确认密码"
                        show-password
                        type="password"
                        autocomplete="new-password"
                        @keyup.enter="submitRegister"
                      >
                        <template #prefix>
                          <Icon icon="iconoir:check-circle" class="input-icon" />
                        </template>
                      </el-input>
                    </el-form-item>

                    <el-form-item>
                      <el-button :loading="loading" class="panel__primary" type="primary" @click="submitRegister" round>
                        立即注册
                        <Icon icon="iconoir:user-plus" class="btn-icon" />
                      </el-button>
                    </el-form-item>
                  </el-form>
                </div>
              </el-tab-pane>
            </el-tabs>

            <div class="panel__bottom">
              <div class="panel__bottom-row">
                <Icon class="panel__bottom-icon" icon="iconoir:info-circle" />
                <span>遇到问题？请联系管理员或您的任课老师。</span>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  background: #f6f8fa;
  overflow: hidden;
}

/* Left Side Styles */
.intro-side {
  flex: 0 0 45%;
  background: #1a1e23;
  color: #fff;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 60px 80px;
  overflow: hidden;
}

.intro-bg {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 10% 20%, rgba(16, 212, 166, 0.1), transparent 40%),
              radial-gradient(circle at 90% 80%, rgba(255, 161, 22, 0.1), transparent 40%);
  opacity: 0.6;
}

.intro-content {
  position: relative;
  z-index: 2;
  max-width: 500px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 60px;
}

.logo-box {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #10d4a6, #0cbdb9);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 16px rgba(16, 212, 166, 0.2);
}

.logo-icon-lg {
  font-size: 32px;
  color: #fff;
}

.brand-text {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.main-title {
  font-size: 56px;
  line-height: 1.1;
  font-weight: 800;
  margin-bottom: 24px;
}

.highlight {
  background: linear-gradient(120deg, #10d4a6, #ffa116);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.sub-desc {
  font-size: 18px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 60px;
}

.features {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.feature-icon {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #10d4a6;
  flex-shrink: 0;
}

.feature-text h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px;
}

.feature-text p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

.intro-footer {
  position: absolute;
  bottom: 40px;
  left: 80px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.3);
}

/* Right Side Styles */
.form-side {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.page__bg {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 80% 10%, rgba(16, 212, 166, 0.08), transparent 40%),
    radial-gradient(circle at 10% 90%, rgba(255, 161, 22, 0.08), transparent 40%);
  pointer-events: none;
}

.layout {
  width: 100%;
  max-width: 440px;
  position: relative;
  z-index: 1;
}

.panel {
  width: 100%;
}

.panel__header {
  text-align: center;
  margin-bottom: 32px;
}

.mobile-logo {
  display: none;
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, rgba(16, 212, 166, 0.1), rgba(255, 161, 22, 0.1));
  border-radius: 16px;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.welcome-title {
  font-size: 28px;
  font-weight: 800;
  color: #1a1e23;
  margin: 0 0 8px;
}

.welcome-desc {
  font-size: 15px;
  color: #666;
  margin: 0;
}

/* Panel Card & Forms (Reused & Adjusted) */
.panel__card {
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.04);
  backdrop-filter: blur(20px);
}

.panel__card :deep(.el-card__body) {
  padding: 32px;
}

.panel__tabs :deep(.el-tabs__item) {
  font-size: 16px;
  color: #666;
}

.panel__tabs :deep(.el-tabs__item.is-active) {
  color: #1a1e23;
  font-weight: 700;
}

.panel__primary {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #10d4a6, #0cbdb9);
  border: none;
  font-size: 16px;
  font-weight: 600;
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.panel__primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(16, 212, 166, 0.3);
}

/* Captcha & Inputs */
.captcha-row {
  display: flex;
  gap: 12px;
}

.captcha-img-wrapper {
  flex: 0 0 120px;
  height: 40px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
  cursor: pointer;
}

.panel__bottom {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px dashed #eee;
  color: #888;
  font-size: 13px;
  text-align: center;
}

.panel__bottom-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.mb-4 { margin-bottom: 16px; }

/* Decorative Elements */
.decorative-circle {
  position: absolute;
  border-radius: 50%;
}

.circle-1 {
  width: 400px;
  height: 400px;
  top: -100px;
  right: -100px;
  background: radial-gradient(circle, rgba(16, 212, 166, 0.05), transparent 70%);
  animation: float 8s ease-in-out infinite;
}

.circle-2 {
  width: 300px;
  height: 300px;
  bottom: -50px;
  left: -50px;
  background: radial-gradient(circle, rgba(255, 161, 22, 0.05), transparent 70%);
  animation: float 10s ease-in-out infinite reverse;
}

.decorative-shape {
  position: absolute;
  background: linear-gradient(45deg, rgba(255, 255, 255, 0.03), transparent);
}

.shape-1 {
  width: 200px;
  height: 200px;
  top: 40%;
  left: 10%;
  clip-path: polygon(50% 0%, 100% 50%, 50% 100%, 0% 50%);
  animation: rotate 30s linear infinite;
}

/* Responsive */
@media (max-width: 1024px) {
  .intro-side {
    display: none;
  }
  
  .form-side {
    flex: 1;
  }
  
  .mobile-logo {
    display: flex;
  }
}

/* Animations */
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes fadeInRight {
  from { opacity: 0; transform: translateX(20px); }
  to { opacity: 1; transform: translateX(0); }
}

.fade-in-right {
  animation: fadeInRight 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.fade-in {
  animation: fadeIn 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
