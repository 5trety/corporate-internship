import{$ as e,F as t,G as n,H as r,I as i,K as a,L as o,M as ee,P as s,R as c,S as te,W as l,X as u,Z as d,a as f,c as p,et as ne,it as m,o as h,q as re,s as g,tt as _,z as v}from"./index-Dk1xSS8z.js";import{f as y,i as b,p as x,v as S}from"./wms-CSAjFvew.js";import{t as C}from"./browser-Cs6qecrv.js";var w=m(C()),ie={class:`wms-page`},ae={class:`page-header`},T={class:`mobile-cards`},E={class:`card-header`},D={class:`order-no`},O={class:`card-body`},oe={class:`info-row`},se={class:`info-row`},ce={class:`info-row`},le={class:`info-row`},ue={class:`info-row`},de={class:`card-actions`},fe=[`src`],pe={key:1},me={style:{"margin-top":`20px`,"text-align":`center`}},k=h({__name:`OutboundOrderList`,setup(m){let h=f(),C=e(!1),k=e([]),A=e(0),j=e(1),M=e(20),N=e(``),P=e([]),F=e(!1),I=e({}),L=e([]),R=e(!1),z=e([]),B=e(null),V=e=>{switch(e){case`pending`:return`warning`;case`partial`:return`primary`;case`completed`:return`success`;default:return`info`}},H=e=>Number(e||0),U=e=>{let t=H(e.expected_quantity??e.expectedQuantity),n=H(e.shipped_quantity??e.shippedQuantity),r=Math.max(0,t-n),i=H(e.expected_boxes??e.expectedBoxes),a=H(e.shipped_boxes??e.shippedBoxes),o=Math.max(1,i-a);return{partCode:e.part_code||e.partCode,partName:e.part_name||e.partName,remainingQuantity:r,expectedQuantity:r,expectedBoxes:o,kanbanNo:``,qrCodeImage:null}},W=async()=>{C.value=!0;try{let e=await x({page:j.value,pageSize:M.value,status:N.value,startDate:P.value?.[0],endDate:P.value?.[1]});e.code===200&&(k.value=e.data?.list||[],A.value=e.data?.total||0)}finally{C.value=!1}},he=()=>{N.value=``,P.value=[],j.value=1,W()},G=async e=>{let t=await y(e.orderNo);t.code===200&&(I.value=t.data?.order||{},L.value=t.data?.details||[],F.value=!0)},K=e=>{h.push(`/wms-outbound/outbound-order/edit/${e.orderNo}`)},q=e=>{g.confirm(`确定删除出库单"${e.orderNo}"吗？`,`提示`,{confirmButtonText:`确定`,cancelButtonText:`取消`,type:`warning`}).then(async()=>{let t=await b(e.orderNo);t.code===200?(p.success(`删除成功`),W()):p.error(t.message||`删除失败`)})},J=async e=>{B.value=e;let t=await y(e.orderNo);if(t.code===200){let e=t.data?.details||[];console.log(`出库单详情:`,e);let n=e.map(U).filter(e=>e.remainingQuantity>0);if(n.length===0){p.warning(`该出库单明细已全部完成，无需打印看板`);return}z.value=n,R.value=!0}else p.error(`获取出库单详情失败`)},Y=async e=>{try{return await w.toDataURL(e,{width:120,margin:1,errorCorrectionLevel:`M`})}catch(e){return console.error(`生成二维码失败:`,e),null}},ge=async()=>{if(!B.value)return;console.log(`准备生成看板的kanbanItems:`,z.value);let e=z.value.map(e=>({partCode:e.partCode,partName:e.partName,expectedQuantity:e.remainingQuantity,quantity:e.expectedQuantity,boxCount:e.expectedBoxes||1}));console.log(`发送到后端的数据:`,{orderNo:B.value.orderNo,items:e});let t=p.info({message:`正在生成看板和二维码...`,duration:0});try{let n=await S({orderNo:B.value.orderNo,items:e});if(n.code===200){let e=n.data||[];p.success(`成功生成 ${e.length} 个看板`);for(let t=0;t<z.value.length;t++)if(e[t]){z.value[t].kanbanNo=e[t].kanbanNo;let n=e[t].kanbanNo;z.value[t].qrCodeImage=await Y(n)}t.close();let r=window.open(``,`_blank`);r.document.write(`
        <html>
          <head>
            <title>打印出库看板</title>
            <style>
              body { font-family: Arial, sans-serif; padding: 20px; }
              .kanban-card {
                border: 2px solid #333;
                padding: 20px;
                margin-bottom: 20px;
                width: 350px;
                page-break-after: always;
              }
              .kanban-title { font-size: 18px; font-weight: bold; text-align: center; margin-bottom: 10px; }
              .kanban-code { font-size: 12px; color: #666; text-align: center; margin-bottom: 15px; word-break: break-all; }
              .kanban-row { margin: 8px 0; white-space: nowrap; }
              .kanban-label { font-weight: bold; display: inline-block; width: 70px; margin-right: 10px; }
              .kanban-value { display: inline-block; }
              .qr-code { text-align: center; margin: 15px 0; }
              .footer { text-align: center; font-size: 10px; color: #999; margin-top: 10px; }
            </style>
          </head>
          <body>
            ${z.value.map((e,t)=>`
              <div class="kanban-card">
                <div class="kanban-title">出库看板</div>
                <div class="kanban-code">${e.kanbanNo}</div>
                <div class="kanban-row"><span class="kanban-label">零件号：</span><span class="kanban-value">${e.partCode}</span></div>
                <div class="kanban-row"><span class="kanban-label">零件名称：</span><span class="kanban-value">${e.partName}</span></div>
                <div class="kanban-row"><span class="kanban-label">数量：</span><span class="kanban-value">${e.expectedQuantity}</span></div>
                <div class="kanban-row"><span class="kanban-label">客户：</span><span class="kanban-value">${B.value.customerName}</span></div>
                <div class="kanban-row"><span class="kanban-label">出库单号：</span><span class="kanban-value">${B.value.orderNo}</span></div>
                <div class="qr-code">
                  <img src="${e.qrCodeImage}" style="width: 100px; height: 100px;" alt="二维码">
                  <div style="font-size: 10px; margin-top: 5px;">扫码出库</div>
                </div>
                <div class="footer">生成时间：${new Date().toLocaleString()}</div>
              </div>
            `).join(``)}
          </body>
        </html>
      `),r.document.close(),r.print()}else t.close(),p.error(n.message||`生成看板失败`)}catch(e){t.close(),console.error(`生成看板失败:`,e),p.error(`生成看板失败: `+(e.message||`未知错误`))}},_e=async e=>{if(!e.kanbanNo){p.warning(`请先批量生成看板`);return}let t=e.qrCodeImage;if(!t){let n=e.kanbanNo;t=await Y(n)}let n=window.open(``,`_blank`);n.document.write(`
    <html>
      <head>
        <title>打印出库看板 - ${e.kanbanNo}</title>
        <style>
          body { font-family: Arial, sans-serif; padding: 20px; }
          .kanban-card {
            border: 2px solid #333;
            padding: 20px;
            width: 350px;
            margin: 0 auto;
          }
          .kanban-title { font-size: 18px; font-weight: bold; text-align: center; margin-bottom: 10px; }
          .kanban-code { font-size: 12px; color: #666; text-align: center; margin-bottom: 15px; word-break: break-all; }
          .kanban-row { margin: 8px 0; white-space: nowrap; }
          .kanban-label { font-weight: bold; display: inline-block; width: 70px; margin-right: 10px; }
          .kanban-value { display: inline-block; }
          .qr-code { text-align: center; margin: 15px 0; }
          .footer { text-align: center; font-size: 10px; color: #999; margin-top: 10px; }
        </style>
      </head>
      <body>
        <div class="kanban-card">
          <div class="kanban-title">出库看板</div>
          <div class="kanban-code">${e.kanbanNo}</div>
          <div class="kanban-row"><span class="kanban-label">零件号：</span><span class="kanban-value">${e.partCode}</span></div>
          <div class="kanban-row"><span class="kanban-label">零件名称：</span><span class="kanban-value">${e.partName}</span></div>
          <div class="kanban-row"><span class="kanban-label">数量：</span><span class="kanban-value">${e.expectedQuantity}</span></div>
          <div class="kanban-row"><span class="kanban-label">客户：</span><span class="kanban-value">${B.value?.customerName}</span></div>
          <div class="kanban-row"><span class="kanban-label">出库单号：</span><span class="kanban-value">${B.value?.orderNo}</span></div>
          <div class="qr-code">
            <img src="${t}" style="width: 100px; height: 100px;" alt="二维码">
            <div style="font-size: 10px; margin-top: 5px;">扫码出库</div>
          </div>
          <div class="footer">生成时间：${new Date().toLocaleString()}</div>
        </div>
      </body>
    </html>
  `),n.document.close(),n.print()};return r(()=>{W()}),(e,r)=>{let f=a(`el-icon`),p=a(`el-button`),m=a(`el-option`),h=a(`el-select`),g=a(`el-form-item`),y=a(`el-date-picker`),b=a(`el-form`),x=a(`el-table-column`),S=a(`el-progress`),w=a(`el-tag`),B=a(`el-table`),H=a(`el-card`),U=a(`el-empty`),Y=a(`el-pagination`),X=a(`el-descriptions-item`),ve=a(`el-descriptions`),Z=a(`el-divider`),Q=a(`el-dialog`),ye=a(`el-input`),$=re(`loading`);return l(),o(`div`,ie,[s(`div`,ae,[r[9]||=s(`h2`,null,`出库单管理`,-1),v(p,{type:`primary`,onClick:r[0]||=t=>e.$router.push(`/wms-outbound/outbound-order/create`)},{default:u(()=>[v(f,null,{default:u(()=>[v(ne(te))]),_:1}),r[8]||=c(` 创建出库单 `,-1)]),_:1})]),v(H,{shadow:`never`},{default:u(()=>[v(b,{inline:!0,class:`search-form`},{default:u(()=>[v(g,{label:`状态`},{default:u(()=>[v(h,{modelValue:N.value,"onUpdate:modelValue":r[1]||=e=>N.value=e,placeholder:`全部`,clearable:``,style:{width:`120px`},onChange:W},{default:u(()=>[v(m,{label:`待出库`,value:`pending`}),v(m,{label:`部分出库`,value:`partial`}),v(m,{label:`已完成`,value:`completed`})]),_:1},8,[`modelValue`])]),_:1}),v(g,{label:`日期范围`},{default:u(()=>[v(y,{modelValue:P.value,"onUpdate:modelValue":r[2]||=e=>P.value=e,type:`daterange`,"range-separator":`至`,"start-placeholder":`开始日期`,"end-placeholder":`结束日期`,"value-format":`YYYY-MM-DD`},null,8,[`modelValue`])]),_:1}),v(g,null,{default:u(()=>[v(p,{type:`primary`,onClick:W},{default:u(()=>[...r[10]||=[c(`搜索`,-1)]]),_:1}),v(p,{onClick:he},{default:u(()=>[...r[11]||=[c(`重置`,-1)]]),_:1})]),_:1})]),_:1}),d((l(),t(B,{data:k.value,stripe:``,class:`desktop-table`},{default:u(()=>[v(x,{prop:`orderNo`,label:`出库单号`,width:`180`}),v(x,{prop:`outboundType`,label:`出库类型`,width:`120`}),v(x,{prop:`customerName`,label:`客户`,width:`150`}),v(x,{prop:`totalQuantity`,label:`总数量`,width:`100`}),v(x,{prop:`shippedQuantity`,label:`已出库`,width:`100`}),v(x,{label:`进度`,width:`150`},{default:u(({row:e})=>[v(S,{percentage:Math.round(e.shippedQuantity/e.totalQuantity*100),"stroke-width":8},null,8,[`percentage`])]),_:1}),v(x,{prop:`statusText`,label:`状态`,width:`100`},{default:u(({row:e})=>[v(w,{type:V(e.status)},{default:u(()=>[c(_(e.statusText),1)]),_:2},1032,[`type`])]),_:1}),v(x,{prop:`createdBy`,label:`创建人`,width:`120`}),v(x,{prop:`createdAt`,label:`创建时间`,width:`180`}),v(x,{label:`操作`,width:`320`,fixed:`right`},{default:u(({row:e})=>[v(p,{link:``,type:`primary`,onClick:t=>G(e)},{default:u(()=>[...r[12]||=[c(`查看`,-1)]]),_:1},8,[`onClick`]),e.status===`completed`?i(``,!0):(l(),t(p,{key:0,link:``,type:`primary`,onClick:t=>K(e)},{default:u(()=>[...r[13]||=[c(`编辑`,-1)]]),_:1},8,[`onClick`])),e.status===`completed`?i(``,!0):(l(),t(p,{key:1,link:``,type:`success`,onClick:t=>J(e)},{default:u(()=>[...r[14]||=[c(`打印看板`,-1)]]),_:1},8,[`onClick`])),e.status===`completed`?i(``,!0):(l(),t(p,{key:2,link:``,type:`danger`,onClick:t=>q(e)},{default:u(()=>[...r[15]||=[c(`删除`,-1)]]),_:1},8,[`onClick`]))]),_:1})]),_:1},8,[`data`])),[[$,C.value]]),d((l(),o(`div`,T,[(l(!0),o(ee,null,n(k.value,e=>(l(),t(H,{key:e.orderNo,shadow:`hover`,class:`order-card`},{default:u(()=>[s(`div`,E,[s(`div`,D,_(e.orderNo),1),v(w,{type:V(e.status),size:`small`},{default:u(()=>[c(_(e.statusText),1)]),_:2},1032,[`type`])]),s(`div`,O,[s(`div`,oe,[r[16]||=s(`span`,{class:`label`},`出库类型：`,-1),s(`span`,null,_(e.outboundType),1)]),s(`div`,se,[r[17]||=s(`span`,{class:`label`},`客户：`,-1),s(`span`,null,_(e.customerName),1)]),s(`div`,ce,[r[18]||=s(`span`,{class:`label`},`数量：`,-1),s(`span`,null,_(e.shippedQuantity)+` / `+_(e.totalQuantity),1)]),s(`div`,le,[r[19]||=s(`span`,{class:`label`},`进度：`,-1),v(S,{percentage:Math.round(e.shippedQuantity/e.totalQuantity*100),"stroke-width":6,style:{flex:`1`}},null,8,[`percentage`])]),s(`div`,ue,[r[20]||=s(`span`,{class:`label`},`创建时间：`,-1),s(`span`,null,_(e.createdAt),1)])]),s(`div`,de,[v(p,{size:`small`,onClick:t=>G(e)},{default:u(()=>[...r[21]||=[c(`查看`,-1)]]),_:1},8,[`onClick`]),e.status===`completed`?i(``,!0):(l(),t(p,{key:0,size:`small`,type:`primary`,onClick:t=>K(e)},{default:u(()=>[...r[22]||=[c(`编辑`,-1)]]),_:1},8,[`onClick`])),e.status===`completed`?i(``,!0):(l(),t(p,{key:1,size:`small`,type:`success`,onClick:t=>J(e)},{default:u(()=>[...r[23]||=[c(`打印`,-1)]]),_:1},8,[`onClick`])),e.status===`completed`?i(``,!0):(l(),t(p,{key:2,size:`small`,type:`danger`,onClick:t=>q(e)},{default:u(()=>[...r[24]||=[c(`删除`,-1)]]),_:1},8,[`onClick`]))])]),_:2},1024))),128)),k.value.length===0&&!C.value?(l(),t(U,{key:0,description:`暂无数据`})):i(``,!0)])),[[$,C.value]]),v(Y,{"current-page":j.value,"onUpdate:currentPage":r[3]||=e=>j.value=e,"page-size":M.value,"onUpdate:pageSize":r[4]||=e=>M.value=e,total:A.value,"page-sizes":[10,20,50,100],layout:`total, sizes, prev, pager, next, jumper`,onSizeChange:W,onCurrentChange:W},null,8,[`current-page`,`page-size`,`total`])]),_:1}),v(Q,{title:`出库单详情`,modelValue:F.value,"onUpdate:modelValue":r[5]||=e=>F.value=e,width:`800px`},{default:u(()=>[v(ve,{column:2,border:``},{default:u(()=>[v(X,{label:`出库单号`},{default:u(()=>[c(_(I.value.orderNo),1)]),_:1}),v(X,{label:`出库类型`},{default:u(()=>[c(_(I.value.outboundType),1)]),_:1}),v(X,{label:`客户`},{default:u(()=>[c(_(I.value.customerName),1)]),_:1}),v(X,{label:`仓库`},{default:u(()=>[c(_(I.value.warehouseName),1)]),_:1}),v(X,{label:`总数量`},{default:u(()=>[c(_(I.value.totalQuantity),1)]),_:1}),v(X,{label:`已出库`},{default:u(()=>[c(_(I.value.shippedQuantity),1)]),_:1}),v(X,{label:`总箱数`},{default:u(()=>[c(_(I.value.totalBoxes),1)]),_:1}),v(X,{label:`已出库箱数`},{default:u(()=>[c(_(I.value.shippedBoxes),1)]),_:1}),v(X,{label:`状态`,span:2},{default:u(()=>[v(w,{type:V(I.value.status)},{default:u(()=>[c(_(I.value.statusText),1)]),_:1},8,[`type`])]),_:1}),v(X,{label:`备注`,span:2},{default:u(()=>[c(_(I.value.remark),1)]),_:1})]),_:1}),v(Z,null,{default:u(()=>[...r[25]||=[c(`出库明细`,-1)]]),_:1}),v(B,{data:L.value,stripe:``,size:`small`},{default:u(()=>[v(x,{prop:`part_code`,label:`零件号`,width:`180`}),v(x,{prop:`part_name`,label:`零件名称`}),v(x,{prop:`expected_quantity`,label:`预期数量`,width:`100`}),v(x,{prop:`shipped_quantity`,label:`已出库`,width:`100`}),v(x,{prop:`expected_boxes`,label:`预期箱数`,width:`100`}),v(x,{prop:`shipped_boxes`,label:`已出库箱数`,width:`100`}),v(x,{label:`进度`,width:`150`},{default:u(({row:e})=>[v(S,{percentage:Math.round(e.shipped_quantity/e.expected_quantity*100),"stroke-width":6},null,8,[`percentage`])]),_:1})]),_:1},8,[`data`])]),_:1},8,[`modelValue`]),v(Q,{title:`打印出库看板`,modelValue:R.value,"onUpdate:modelValue":r[7]||=e=>R.value=e,width:`900px`},{footer:u(()=>[v(p,{onClick:r[6]||=e=>R.value=!1},{default:u(()=>[...r[28]||=[c(`关闭`,-1)]]),_:1})]),default:u(()=>[v(B,{data:z.value,stripe:``},{default:u(()=>[v(x,{prop:`partCode`,label:`零件号`,width:`150`}),v(x,{prop:`partName`,label:`零件名称`,width:`150`}),v(x,{prop:`expectedQuantity`,label:`剩余数量`,width:`100`}),v(x,{prop:`expectedBoxes`,label:`剩余箱数`,width:`100`}),v(x,{label:`看板号`,width:`220`},{default:u(({row:e})=>[v(ye,{modelValue:e.kanbanNo,"onUpdate:modelValue":t=>e.kanbanNo=t,placeholder:`自动生成`,disabled:``},null,8,[`modelValue`,`onUpdate:modelValue`])]),_:1}),v(x,{label:`二维码`,width:`120`},{default:u(({row:e})=>[e.qrCodeImage?(l(),o(`img`,{key:0,src:e.qrCodeImage,style:{width:`80px`,height:`80px`}},null,8,fe)):(l(),o(`span`,pe,`生成中...`))]),_:1}),v(x,{label:`操作`,width:`100`},{default:u(({row:e})=>[v(p,{link:``,type:`primary`,size:`small`,onClick:t=>_e(e)},{default:u(()=>[...r[26]||=[c(`打印`,-1)]]),_:1},8,[`onClick`])]),_:1})]),_:1},8,[`data`]),s(`div`,me,[v(p,{type:`primary`,onClick:ge},{default:u(()=>[...r[27]||=[c(`批量生成并打印`,-1)]]),_:1})])]),_:1},8,[`modelValue`])])}}},[[`__scopeId`,`data-v-db181f24`]]);export{k as default};