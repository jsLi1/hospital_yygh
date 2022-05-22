package com.atguigu.yygh.cmn.service.impl;

import com.alibaba.excel.EasyExcel;
import com.atguigu.yygh.cmn.listener.DictListener;
import com.atguigu.yygh.cmn.mapper.DictMapper;
import com.atguigu.yygh.cmn.service.DictService;
import com.atguigu.yygh.model.cmn.Dict;
import com.atguigu.yygh.vo.cmn.DictEeVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@Service
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements DictService {

    @Override
    @Cacheable(value = "dict",keyGenerator = "keyGenerator")
    public List<Dict> findChildData(Long id) {
        QueryWrapper<Dict> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("parent_id",id);
        var dictList = baseMapper.selectList(queryWrapper);
        //向list集合每个dict对象中设置hashchildern
        for(Dict dict:dictList){
            var dictId = dict.getId();
            var children = this.isChildren(dictId);
            dict.setHasChildren(children);
        }
        return dictList;
    }

    @Override
    public void exportDictData(HttpServletResponse response) {
        //设置下载信息
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
        String fileName = "dict";
        response.setHeader("Content-disposition", "attachment;filename="+ fileName + ".xlsx");
        //查询数据库
        var dictList = baseMapper.selectList(null);
        //dict-dictvo
        List<DictEeVo> dictEeVos=new ArrayList<>();
        for(Dict dict:dictList){
            DictEeVo dictEeVo=new DictEeVo();
            BeanUtils.copyProperties(dict,dictEeVo);
            dictEeVos.add(dictEeVo);
        }
        //调用方法写
        try {
            EasyExcel.write(response.getOutputStream(),DictEeVo.class).sheet("dict")
                    .doWrite(dictEeVos);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    @CacheEvict(value = "dict",allEntries = true)
    public void importDictData(MultipartFile multipartFile) {
        try {
            EasyExcel.read(multipartFile.getInputStream(),DictEeVo.class,new DictListener(baseMapper))
                    .sheet().doRead();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getDictName(String dictCode, String value) {
         //如果diceCode为空，根据value查询
        if(StringUtils.isEmpty(dictCode)){
            //直接根据value查询
            QueryWrapper<Dict> wrapper=new QueryWrapper<>();
            wrapper.eq("value",value);
            Dict dict=baseMapper.selectOne(wrapper);
            return dict.getName();
        }else{
            Dict dict=this.getDictByDictCode(dictCode);
            var parent_id = dict.getId();
            //根据paraent_id和value进行查询
            var finalDict = baseMapper.selectOne(new QueryWrapper<Dict>()
                    .eq("parent_id", parent_id).eq("value", value));
            return finalDict.getName();

        }
    }

    @Override
    public List<Dict> findByDictCode(String dictCode) {
        //现根据dictcode获取对应的id
       Dict dict=this.getDictByDictCode(dictCode);
       //根据id获取子节点
        var list = this.findChildData(dict.getId());
        return list;

    }

    private Dict getDictByDictCode(String dictCode){
        QueryWrapper<Dict> wrapper=new QueryWrapper<>();
        wrapper.eq("dict_code",dictCode);
        Dict codeDict=baseMapper.selectOne(wrapper);
        return codeDict;
    }

    //判断id下面是否有子节点
    private boolean isChildren(Long id){
        QueryWrapper<Dict> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("parent_id",id);
        var count = baseMapper.selectCount(queryWrapper);
        return count>0;
    }
}
