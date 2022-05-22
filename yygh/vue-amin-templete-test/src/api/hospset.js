import request from '@/utils/request'

export default {
  //医院设置方法
  getHospSetList(current,limit,searchObj){
      return request({
          url:`/admin/hosp/hospitalSet/findPage/${current}/${limit}`,
          method: 'post',
          data:searchObj//使用json形式传递
      })
  },
  //删除医院
  deleteHospSet(id){
    return request({
      url:`/admin/hosp/hospitalSet/${id}`,
      method: 'delete',
    })
  },
    //批量删除医院
    batchdeleteHospSet(idList){
      return request({
        url:`/admin/hosp/hospitalSet/batchRemove`,
        method: 'delete',
        data:idList
      })
    },
    //锁定和取消锁定
    lockHospSet(id,status){
      return request({
        url:`/admin/hosp/hospitalSet/lockHospitalSet/${id}/${status}`,
        method: 'put',
      })
    },
    //添加医院操作
    saveHospSet(hospitalSet){
      return request({
        url:`/admin/hosp/hospitalSet/saveHospitalSet`,
        method: 'post',
        data:hospitalSet
      })
    },
    //院设置查询
    getHospSet(id){
      return request({
        url:`/admin/hosp/hospitalSet/getHospSet/${id}`,
        method: 'get',
      })
    },
    //修改医院操作
    updateHospSet(hospitalSet){
      return request({
        url:`/admin/hosp/hospitalSet/updateHospitalSet`,
        method: 'post',
        data:hospitalSet
      })
    }

}
