<template>
  <div ref="mypage">
    <div v-loading="g_loading" style="width: calc(100% - 2px);height:calc(100vh);">
      <RelationGraph ref="graphRef" :options="userGraphOptions">
        <template #node="{node}">
          <div v-if="node.data.type === 'idc-service1'" style="display: flex; justify-content: center; align-items: center; height: 100%" @click="showNodeTips(node, $event)">
            <div style="text-align: center;" @mouseout="hideNodeTips(node, $event)">
              {{ node.text }}
            </div>
          </div>
        </template>
      </RelationGraph>
    </div>
    <detail-dialog :visible.sync="showDialog" :sub="sub" :title="title" />
  </div>
</template>

<script>
// 如果您没有在main.js文件中使用Vue.use(RelationGraph); 就需要使用下面这一行代码来引入relation-graph
import RelationGraph from 'relation-graph'
import DetailDialog from '@/views/net-topology/components/detail.vue'

export default {
  name: 'NT',
  components: { DetailDialog, RelationGraph },
  data() {
    return {
      data: [
        { name: '流量', subset: [
          { name: 'gateway<br>bkrepo.woa.com', desc: '' },
          { name: 'gateway<br>devnet.bkrepo.woa.com', desc: '' },
          { name: 'gateway<br>devx.bkrepo.woa.com', desc: '' }
        ] },
        { name: 'gateway<br>bkrepo.woa.com', subset: [
          { name: 'IDC service', desc: '' }
        ] },
        { name: 'IDC service', hasIgnore: true, subset: [
          { name: 'IDC storage', desc: '' }
        ], ignoreSub: [
          { name: 'IDC service1', desc: '' },
          { name: 'IDC service2', desc: '' },
          { name: 'IDC service3', desc: '' }
        ] },
        { name: 'IDC storage', hasIgnore: true, subset: [
        ], ignoreSub: [
          { name: 'IDC storage1', desc: '' },
          { name: 'IDC storage2', desc: '' },
          { name: 'IDC storage3', desc: '' },
          { name: 'IDC storage4', desc: '' },
          { name: 'IDC storage5', desc: '' },
          { name: 'IDC storage6', desc: '' },
          { name: 'IDC storage7', desc: '' },
          { name: 'IDC storage8', desc: '' },
          { name: 'IDC storage9', desc: '' },
          { name: 'IDC storage10', desc: '' },
          { name: 'IDC storage11', desc: '' },
          { name: 'IDC storage12', desc: '' },
          { name: 'IDC storage13', desc: '' },
          { name: 'IDC storage14', desc: '' },
          { name: 'IDC storageN', desc: '' }
        ] },
        { name: 'gateway<br>devnet.bkrepo.woa.com', subset: [
          { name: 'DEVNET service1', desc: '' }
        ] },
        { name: 'gateway<br>devx.bkrepo.woa.com', subset: [
          { name: 'DEVX service1', desc: '' }
        ] },
        { name: 'DEVNET service1', subset: [
          { name: 'DEVNET storage1', desc: '' },
          { name: 'DEVNET storage_1', desc: '' }
        ] },
        { name: 'DEVNET storage1', subset: [
        ] },
        { name: 'DEVNET storage_1', subset: [
        ] },
        { name: 'DEVX service1', subset: [
          { name: 'DEVX storage1', desc: '' },
          { name: 'DEVX storage2', desc: '' },
          { name: 'DEVX storage3', desc: '' },
          { name: 'DEVX storage4', desc: '' },
          { name: 'DEVX storage5', desc: '' },
          { name: 'DEVX storage6', desc: '' },
          { name: 'DEVX storage7', desc: '' },
          { name: 'DEVX storage8', desc: '' },
          { name: 'DEVX storageN', desc: '' }
        ] },
        { name: 'DEVX storage1', subset: [
        ] },
        { name: 'DEVX storage2', subset: [
        ] },
        { name: 'DEVX storage3', subset: [
        ] },
        { name: 'DEVX storage4', subset: [
        ] },
        { name: 'DEVX storage5', subset: [
        ] },
        { name: 'DEVX storage6', subset: [
        ] },
        { name: 'DEVX storage7', subset: [
        ] },
        { name: 'DEVX storage8', subset: [
        ] },
        { name: 'DEVX storageN', subset: [
        ] }
      ],
      targetJson: {
        'nodes': [],
        'lines': []
      },
      showDialog: false,
      sub: [],
      title: '',
      isShowCodePanel: false,
      isShowNodeTipsPanel: false,
      nodeMenuPanelPosition: { x: 0, y: 0 },
      currentNode: {},
      g_loading: true,
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
  created() {
  },
  mounted() {
    this.setGraphData()
  },
  methods: {
    dealJson() {
      const relation = this.findNodesWithMultipleParents(this.data)
      for (let i = 0; i < this.data.length; i++) {
        const node = {
          text: this.data[i].name,
          id: this.data[i].name,
          nodeShape: 1,
          width: 150,
          height: 50
        }
        if (this.data[i].hasIgnore) {
          node.data = {
            type: 'idc-service1',
            sub: this.data[i].ignoreSub
          }
        }
        let expand = false
        let hasMulti = false
        if (this.data[i].subset.length > 0) {
          expand = true
          for (let j = 0; j < this.data[i].subset.length; j++) {
            const line = {
              from: this.data[i].name,
              to: this.data[i].subset[j].name,
              text: this.data[i].subset[j].desc
            }
            if (relation.get(this.data[i].subset[j].name).size > 1) {
              line.lineShape = 4
              hasMulti = true
            }
            if (this.data[i].subset[j].desc !== '') {
              line.animation = 2
              line.lineShape = 4
            }
            this.targetJson.lines.push(line)
          }
        }
        if (expand && !hasMulti) {
          if (this.userGraphOptions.layout.from === 'top') {
            node.expandHolderPosition = 'bottom'
          } else {
            node.expandHolderPosition = 'right'
          }
        }
        this.targetJson.nodes.push(node)
      }
    },
    findNodesWithMultipleParents(data) {
      const childToParents = new Map()
      data.forEach(parentNode => {
        if (parentNode.subset && parentNode.subset.length > 0) {
          parentNode.subset.forEach(childNode => {
            const childName = childNode.name
            if (!childToParents.has(childName)) {
              childToParents.set(childName, new Set())
            }
            childToParents.get(childName).add(parentNode.name)
          })
        }
      })
      return childToParents
    },
    setGraphData() {
      this.dealJson()
      this.g_loading = false
      this.$refs.graphRef.setJsonData(this.targetJson, (graphInstance) => {
        const nodes = graphInstance.getNodes()
        nodes.forEach(node => {
          if (this.targetJson.nodes.some(n => n.fixed && n.id === node.id)) {
            node.x = graphInstance.graphData.rootNode.x + node.x
            node.y = graphInstance.graphData.rootNode.y + node.y
          }
        })
      })
    },
    showNodeTips(nodeObject, $event) {
      this.currentNode = nodeObject
      console.log(nodeObject)
      this.sub = nodeObject.data.sub
      this.title = nodeObject.text
      this.showDialog = true
    },
    hideNodeTips(nodeObject, $event) {
      this.isShowNodeTipsPanel = false
    }
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
