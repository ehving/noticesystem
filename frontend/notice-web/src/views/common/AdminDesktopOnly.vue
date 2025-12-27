<template>
  <div class="page">
    <el-result
      icon="info"
      title="管理端仅支持电脑端访问"
      sub-title="请在桌面设备访问，或前往移动端报表查看数据"
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/m/dashboard')">前往移动端报表</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.page {
  min-height: 70vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
</style>
