<script setup lang="ts">
import { ref, computed } from 'vue'
import { http } from '../api/http'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  examId: number
}>()

const emit = defineEmits(['success', 'close'])

const loading = ref(false)
const progress = ref(0)
const status = ref('')

const form = ref({
  totalScore: 100,
  difficulty: 3,
  questionCount: 20,
  typeConfigs: [
    { type: 1, count: 10, score: 2, name: '单选题' }, // 1: Single
    { type: 2, count: 5, score: 4, name: '多选题' },  // 2: Multiple
  ],
  knowledgePointRequirements: [
     { category: '言语理解', count: 0 },
     { category: '数量关系', count: 0 },
     { category: '判断推理', count: 0 },
     { category: '资料分析', count: 0 },
     { category: '常识判断', count: 0 }
  ],
  weights: {
    difficultyWeight: 0.25,
    coverageWeight: 0.25,
    typeRatioWeight: 0.25,
    scoreWeight: 0.25
  },
  constraints: {
    avoidDuplicates: true,
    examDuration: 120,
    mandatoryQuestionIds: [] as number[]
  }
})

async function handleGenerate() {
  loading.value = true
  progress.value = 0
  status.value = '正在初始化种群...'
  
  // Simulate progress
  const interval = setInterval(() => {
    if (progress.value < 90) {
      progress.value += 10
      if (progress.value > 30) status.value = '正在进行交叉变异...'
      if (progress.value > 60) status.value = '正在计算适应度...'
      if (progress.value > 80) status.value = '正在选择最优解...'
    }
  }, 500)

  try {
    // Transform form data to match backend DTO
    const payload = {
      examId: props.examId,
      totalScore: totalScore.value,
      difficulty: form.value.difficulty,
      questionCount: totalCount.value,
      typeConfigs: form.value.typeConfigs.filter(c => c.count > 0).map(c => ({
        type: c.type,
        count: c.count,
        score: c.score
      })),
      knowledgePointRequirements: form.value.knowledgePointRequirements.reduce((acc, curr) => {
        if (curr.count > 0) acc[curr.category] = curr.count
        return acc
      }, {} as Record<string, number>),
      weights: form.value.weights,
      constraints: form.value.constraints
    }

    await http.post('/api/teacher/exams/genetic-auto-compose', payload)
    
    progress.value = 100
    status.value = '组卷成功！'
    ElMessage.success('智能组卷成功')
    emit('success')
  } catch (e: any) {
    status.value = '组卷失败: ' + (e.message || '未知错误')
    ElMessage.error(e.message || '组卷失败')
    progress.value = 0
  } finally {
    clearInterval(interval)
    loading.value = false
  }
}

const totalCount = computed(() => {
  return form.value.typeConfigs.reduce((sum, c) => sum + c.count, 0)
})

const totalScore = computed(() => {
  return form.value.typeConfigs.reduce((sum, c) => sum + (c.count * c.score), 0)
})

</script>

<template>
  <div class="genetic-compose">
    <div class="form-section">
      <h4>基本要求</h4>
      <el-form :model="form" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
             <el-form-item label="目标难度">
               <el-rate v-model="form.difficulty" allow-half />
             </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考试时长">
              <el-input-number v-model="form.constraints.examDuration" :min="1" /> 分钟
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <div class="form-section">
      <h4>题型配置</h4>
      <el-table :data="form.typeConfigs" border size="small">
        <el-table-column prop="name" label="题型" />
        <el-table-column label="数量">
          <template #default="{ row }">
            <el-input-number v-model="row.count" :min="0" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="分值">
          <template #default="{ row }">
            <el-input-number v-model="row.score" :min="1" size="small" />
          </template>
        </el-table-column>
      </el-table>
      <div class="summary">
        总题数: {{ totalCount }} / 总分: {{ totalScore }}
      </div>
    </div>

    <div class="form-section">
      <h4>知识点覆盖 (可选)</h4>
      <el-table :data="form.knowledgePointRequirements" border size="small" height="200">
        <el-table-column prop="category" label="知识点" />
        <el-table-column label="最少题数">
          <template #default="{ row }">
            <el-input-number v-model="row.count" :min="0" size="small" />
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="form-section">
      <h4>算法权重偏好</h4>
      <el-form label-width="100px">
        <el-form-item label="难度偏差">
          <el-slider v-model="form.weights.difficultyWeight" :min="0" :max="1" :step="0.05" />
        </el-form-item>
        <el-form-item label="知识点覆盖">
          <el-slider v-model="form.weights.coverageWeight" :min="0" :max="1" :step="0.05" />
        </el-form-item>
        <el-form-item label="题型比例">
          <el-slider v-model="form.weights.typeRatioWeight" :min="0" :max="1" :step="0.05" />
        </el-form-item>
        <el-form-item label="总分误差">
          <el-slider v-model="form.weights.scoreWeight" :min="0" :max="1" :step="0.05" />
        </el-form-item>
      </el-form>
    </div>

    <div class="actions">
      <div v-if="loading" class="progress-box">
        <el-progress :percentage="progress" :status="progress === 100 ? 'success' : ''" />
        <div class="status-text">{{ status }}</div>
      </div>
      <el-button v-else type="primary" size="large" @click="handleGenerate" style="width: 100%">
        开始智能组卷
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.genetic-compose {
  padding: 10px;
}
.form-section {
  margin-bottom: 24px;
}
.form-section h4 {
  margin: 0 0 12px 0;
  font-size: 15px;
  color: #333;
  border-left: 4px solid #409eff;
  padding-left: 8px;
}
.summary {
  margin-top: 8px;
  text-align: right;
  font-weight: bold;
  color: #666;
}
.actions {
  margin-top: 32px;
}
.progress-box {
  text-align: center;
}
.status-text {
  margin-top: 8px;
  color: #666;
  font-size: 14px;
}
</style>
