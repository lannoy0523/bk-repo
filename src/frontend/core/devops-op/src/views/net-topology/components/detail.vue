<template>
  <el-dialog title="Service详情" :visible.sync="showDialog" :before-close="close">
    <div style="width: calc(100% - 2px);height:calc(100vh);">
      <RelationGraph ref="graphRef1" :options="userGraphOptions" />
    </div>
  </el-dialog>
</template>

<script>
import RelationGraph from 'relation-graph'

export default {
  name: 'DetailDialog',
  components: { RelationGraph },
  props: {
    visible: Boolean
    /**
     * 仅在更新模式时有值
     */
  },
  data() {
    return {
      showDialog: this.visible,
      userGraphOptions: {
        moveToCenterWhenRefresh: true,
        zoomToFitWhenRefresh: true,
        // defaultLineShape: 4,
        placeOtherGroup: true,
        defaultNodeWidth: 150,
        defaultNodeHeight: 30,
        // debug: true,
        // defaultExpandHolderPosition: 'right',
        reLayoutWhenExpandedOrCollapsed: true,
        useAnimationWhenExpanded: true,
        backgrounImage: '',
        backgrounImageNoRepeat: true,
        layout:
          {
            label: '树',
            layoutName: 'tree',
            layoutClassName: 'seeks-layout-center',
            from: 'top',
            // 通过这4个属性来调整 tree-层级距离&节点距离
            min_per_width: 200,
            max_per_width: 200,
            min_per_height: 40,
            max_per_height: undefined,
            levelDistance: '' // 如果此选项有值，则优先级高于上面那4个选项
          },
        defaultNodeBorderWidth: 0,
        defaultNodeShape: 1,
        // 'allowShowMiniToolBar': false,
        useAnimationWhenRefresh: true,
        defaultJunctionPoint: 'tb',
        defaultLineShape: 2
      }
    }
  },
  watch: {
    visible: function(newVal) {
      if (newVal) {
        this.showDialog = true
        // 对话框显示后再初始化图表
        this.$nextTick(() => {
          setTimeout(() => {
            this.setSecondData()
          }, 100)
        })
      } else {
        this.close()
      }
    }
  },
  mounted() {
  },
  methods: {
    setSecondData() {
      const __graph_json_data = {
        'nodes': [
          { 'text': 'Service总线', 'id': 'cron', nodeShape: 1, width: 130, height: 40, color: '#34A0CE' },
          { 'text': 'Service1', 'id': 'exe-01', nodeShape: 1, width: 130, height: 35 },
          { 'text': 'Service2', 'id': 'exe-02', nodeShape: 1, width: 130, height: 35 },
          { 'text': 'Service3', 'id': 'exe-03', nodeShape: 1, width: 130, height: 35 },
          { 'text': 'Service4', 'id': 'exe-04', nodeShape: 1, width: 130, height: 35, color: '#34A0CE' },
          { 'text': 'Service5', 'id': 'exe-05', nodeShape: 1, width: 130, height: 35, color: '#34A0CE' },
          { 'text': 'Service6', 'id': 'exe-06', nodeShape: 1, width: 130, height: 35 },
          { 'text': 'Service7', 'id': 'exe-07', nodeShape: 1, width: 130, height: 35 },
          { 'text': 'Service8', 'id': 'exe-08', nodeShape: 1, width: 130, height: 35, color: '#F56C6C' },
          { 'text': 'Service9', 'id': 'exe-09', nodeShape: 1, width: 130, height: 35 },
          { 'text': 'Service10', 'id': 'exe-10', nodeShape: 1, width: 130, height: 35 }
        ],
        'lines': [
          { 'from': 'cron', 'to': 'exe-01', 'text': null },
          { 'from': 'cron', 'to': 'exe-02', 'text': null },
          { 'from': 'cron', 'to': 'exe-03', 'text': null },
          { 'from': 'cron', 'to': 'exe-04', 'text': null },
          { 'from': 'cron', 'to': 'exe-05', 'text': null },
          { 'from': 'cron', 'to': 'exe-06', 'text': null },
          { 'from': 'cron', 'to': 'exe-07', 'text': null },
          { 'from': 'cron', 'to': 'exe-08', 'text': null },
          { 'from': 'cron', 'to': 'exe-09', 'text': null },
          { 'from': 'cron', 'to': 'exe-10', 'text': null }
        ]
      }
      this.$nextTick(() => {
        this.$refs.graphRef1.setJsonData(__graph_json_data, (graphInstance) => {
          const nodes = graphInstance.getNodes()
          nodes.forEach(node => {
            if (__graph_json_data.nodes.some(n => n.fixed && n.id === node.id)) {
              node.x = graphInstance.graphData.rootNode.x + node.x
              node.y = graphInstance.graphData.rootNode.y + node.y
            }
          })
        })
      })
    },
    close() {
      this.showDialog = false
      this.$emit('update:visible', false)
    }
  }
}

</script>

<style scoped>

</style>
