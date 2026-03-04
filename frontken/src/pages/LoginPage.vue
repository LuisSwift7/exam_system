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
    <div class="page__bg" />
    <div class="layout">
      <div class="panel">
        <el-card class="panel__card" shadow="never">
          <div class="panel__top">
            <div class="panel__title">账号入口</div>
            <div class="panel__desc">师生/管理员通用。</div>
          </div>

          <el-tabs v-model="activeTab" class="panel__tabs" stretch>
            <el-tab-pane label="登录" name="login">
              <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top">
                <el-form-item label="账号" prop="username">
                  <el-input v-model="loginForm.username" placeholder="学号 / 工号 / 账号" autocomplete="username">
                    <template #prefix>
                      <Icon icon="iconoir:user" />
                    </template>
                  </el-input>
                </el-form-item>

                <el-form-item label="密码" prop="password">
                  <el-input
                    v-model="loginForm.password"
                    placeholder="输入密码"
                    autocomplete="current-password"
                    show-password
                    type="password"
                    @keyup.enter="submitLogin"
                  >
                    <template #prefix>
                      <Icon icon="iconoir:key" />
                    </template>
                  </el-input>
                </el-form-item>

                <el-form-item label="验证码" prop="captchaCode">
                  <el-row class="captcha" :gutter="10">
                    <el-col :span="15">
                      <el-input v-model="loginForm.captchaCode" placeholder="输入验证码" @keyup.enter="submitLogin">
                        <template #prefix>
                          <Icon icon="iconoir:square" />
                        </template>
                      </el-input>
                    </el-col>
                    <el-col :span="9">
                      <el-image
                        v-if="captcha"
                        class="captcha__img"
                        :src="captcha.imageBase64"
                        fit="cover"
                        @click="refreshCaptcha"
                      />
                      <el-skeleton v-else class="captcha__img" animated />
                    </el-col>
                  </el-row>
                </el-form-item>

                <el-form-item>
                  <el-button :loading="loading" class="panel__primary" type="primary" @click="submitLogin">
                    登录
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="注册" name="register">
              <el-alert :closable="false" show-icon title="仅学生注册。" type="warning" />
              <div class="panel__gap" />
              <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top">
                <el-form-item label="学号" prop="username">
                  <el-input v-model="registerForm.username" placeholder="学号 / 账号" autocomplete="username">
                    <template #prefix>
                      <Icon icon="iconoir:id" />
                    </template>
                  </el-input>
                </el-form-item>

                <el-form-item label="姓名" prop="realName">
                  <el-input v-model="registerForm.realName" placeholder="真实姓名" autocomplete="name">
                    <template #prefix>
                      <Icon icon="iconoir:profile-circle" />
                    </template>
                  </el-input>
                </el-form-item>

                <el-form-item label="密码" prop="password">
                  <el-input
                    v-model="registerForm.password"
                    placeholder="设置密码"
                    show-password
                    type="password"
                    autocomplete="new-password"
                  >
                    <template #prefix>
                      <Icon icon="iconoir:lock" />
                    </template>
                  </el-input>
                </el-form-item>

                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input
                    v-model="registerForm.confirmPassword"
                    placeholder="再输一次"
                    show-password
                    type="password"
                    autocomplete="new-password"
                    @keyup.enter="submitRegister"
                  >
                    <template #prefix>
                      <Icon icon="iconoir:repeat" />
                    </template>
                  </el-input>
                </el-form-item>

                <el-form-item>
                  <el-button :loading="loading" class="panel__primary" type="primary" @click="submitRegister">
                    注册并返回登录
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>

          <div class="panel__bottom">
            <div class="panel__bottom-row">
              <Icon class="panel__bottom-icon" icon="iconoir:warning-triangle" />
              <span>忘记密码找老师。</span>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

.page__bg {
  position: absolute;
  inset: 0;
  background: radial-gradient(980px 640px at 18% 24%, rgba(16, 212, 166, 0.2), transparent 62%),
    radial-gradient(920px 600px at 86% 74%, rgba(255, 161, 22, 0.16), transparent 64%),
    linear-gradient(135deg, #f6f2ea, #eff6f7);
}

.page__bg::after {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0.16;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='140' height='140'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.9' numOctaves='2' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='140' height='140' filter='url(%23n)' opacity='.35'/%3E%3C/svg%3E");
  mix-blend-mode: multiply;
  pointer-events: none;
}

.layout {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  padding: 46px 28px 38px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.panel {
  width: min(520px, 100%);
}

.panel__card {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(18, 24, 28, 0.1);
  box-shadow: 0 20px 60px rgba(10, 16, 20, 0.14);
  backdrop-filter: blur(14px);
}

.panel__card :deep(.el-card__body) {
  padding: 16px 16px 12px;
}

.panel__top {
  margin-bottom: 10px;
}

.panel__title {
  font-size: 18px;
  font-weight: 800;
  color: rgba(16, 18, 20, 0.92);
}

.panel__desc {
  margin-top: 6px;
  font-size: 12px;
  color: rgba(16, 18, 20, 0.62);
}

.panel__tabs :deep(.el-tabs__item) {
  color: rgba(16, 18, 20, 0.62);
}

.panel__tabs :deep(.el-tabs__item.is-active) {
  color: rgba(16, 18, 20, 0.92);
}

.panel__tabs :deep(.el-tabs__active-bar) {
  background-color: rgba(16, 212, 166, 0.92);
}

.panel__primary {
  width: 100%;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(16, 212, 166, 0.92), rgba(255, 161, 22, 0.82));
  border: none;
}

.panel__gap {
  height: 10px;
}

.panel__bottom {
  margin-top: 10px;
  color: rgba(16, 18, 20, 0.58);
  font-size: 12px;
}

.panel__bottom-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel__bottom-icon {
  width: 14px;
  height: 14px;
}

.captcha {
  width: 100%;
}

.captcha__img {
  height: 40px;
  border-radius: 10px;
  cursor: pointer;
}

.panel :deep(.el-input__wrapper) {
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: none;
  border: 1px solid rgba(18, 24, 28, 0.12);
}

.panel :deep(.el-input__inner) {
  color: rgba(16, 18, 20, 0.92);
}

.panel :deep(.el-form-item__label) {
  color: rgba(16, 18, 20, 0.7);
}

.panel :deep(.el-alert) {
  border-radius: 14px;
}

@media (max-width: 900px) {
  .layout {
    padding: 24px 18px 22px;
  }

  .panel {
    width: 100%;
  }
}
</style>

