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
import { credentials } from '@/api/storage'

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
        { name: 'gateway<br>devnet.bkrepo.woa.com', subset: [
          { name: 'gateway<br>bkrepo.woa.com', desc: '是否IDC流量' },
          { name: 'gateway<br>devx.bkrepo.woa.com', desc: '是否IDC流量' },
          { name: 'DEVNET service', desc: '' }
        ] },
        { name: 'gateway<br>bkrepo.woa.com', subset: [
          { name: 'IDC service', desc: '' }
        ] },
        { name: 'IDC service', hasIgnore: true, subset: [
        ], ignoreSub: [
          { name: 'IDC service1', desc: '' },
          { name: 'IDC service2', desc: '' },
          { name: 'IDC service3', desc: '' }
        ] },
        { name: 'gateway<br>devx.bkrepo.woa.com', subset: [
          { name: 'DEVX service', desc: '' }
        ] },
        { name: 'DEVNET service', hasIgnore: true, subset: [
        ], ignoreSub: [
          { name: 'DEVNET service1', desc: '' },
          { name: 'DEVNET service_1', desc: '' }
        ] },
        { name: 'DEVX service', hasIgnore: true, subset: [
        ], ignoreSub: [
          { name: 'DEVX service1', desc: '' },
          { name: 'DEVX service2', desc: '' },
          { name: 'DEVX service3', desc: '' }
        ] }
      ],
      targetJson: {
        'nodes': [],
        'lines': []
      },
      services: [
        'IDC service',
        'DEVNET service',
        'DEVX service'
      ],
      regins: [
        'idc',
        'devnet',
        'devx'
      ],
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
        defaultJunctionPoint: 'border',
        defaultLineShape: 2,
        defaultLineColor: '#000000'
      }
    }
  },
  created() {
    this.getStorage()
  },
  methods: {
    getStorage() {
      credentials().then(res => {
        for (let j = 0; j < this.data.length; j++) {
          if (this.services.includes(this.data[j].name)) {
            const index = this.services.indexOf(this.data[j].name)
            for (let i = 0; i < res.data.length; i++) {
              if (res.data[i].region === this.regins[index]) {
                this.data[j].subset.push({
                  name: res.data[i].key,
                  desc: ''
                })
                this.data.push({
                  name: res.data[i].key,
                  subset: []
                })
              }
            }
          }
        }
        console.log(this.data)
        this.setGraphData()
      })
    },
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
              line.lineShape = 6
              hasMulti = true
            }
            if (this.data[i].subset[j].desc !== '') {
              // line.animation = 1
              line.lineShape = 6
              line.useTextPath = true
              line.color = '#00ced1'
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
