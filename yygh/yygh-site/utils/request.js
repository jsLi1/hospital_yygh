import axios from 'axios'
import { MessageBox, Message } from 'element-ui'
import cookie from 'js-cookie'
// 创建axios实例
const service = axios.create({
    baseURL: 'http://localhost',
    timeout: 15000 // 请求超时时间
})
// http request 拦截器
service.interceptors.request.use(
    config => {
    // token 先不处理，后续使用时在完善
    //先判断cookie中是否有token值
    if(cookie.get("token")){
        //token值放入到cookie中
        config.headers['token']=cookie.get('token')
    }
    return config
},
  err => {
    return Promise.reject(err)
})
// http response 拦截器
service.interceptors.response.use(
    response => {
        if(response.data.code===208){
            //状态码208，弹出登录输入框
            loginEvent.$emit('loginDialogEvent')
            return
        }else{
            if (response.data.code !== 200) {
                Message({
                    message: response.data.message,
                    type: 'error',
                    duration: 5 * 1000
                })
                return Promise.reject(response.data)
            } else {
                return response.data
            }
        }
    },
    error => {
        return Promise.reject(error.response)
})
export default service
