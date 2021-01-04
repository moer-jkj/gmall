package com.atguigu.gmall.ums.service.impl;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.crypto.Digest;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.service.UserService;
import org.springframework.util.CollectionUtils;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        switch (type){
            case 1: wrapper.eq("username",data); break;
            case 2: wrapper.eq("phone",data); break;
            case 3: wrapper.eq("email",data); break;
            default:
                return null;
        }
        return this.count(wrapper) == 0;
    }

    @Override
    public void regist(UserEntity userEntity, String code) {
        // 1.校验短信验证码


        // 2.生成盐
        String salt = StringUtils.substring(UUID.randomUUID().toString(),0,6);
        userEntity.setSalt(salt);
        // 3.对明文密码进行加盐加密
        userEntity.setPassword(DigestUtils.md5Hex(userEntity.getPassword() + salt));
        // 4.新增用户
        userEntity.setLevelId(1l);
        userEntity.setNickname(userEntity.getUsername());
        userEntity.setSourceType(1);
        userEntity.setIntegration(1000);
        userEntity.setGrowth(1000);
        userEntity.setStatus(0);
        userEntity.setCreateTime(new Date());
        this.save(userEntity);
        // 5. 删除reids中的短信验证码
    }

    @Override
    public UserEntity queryUser(String loginName, String password) {

        // 1.根据登录名查询该用户是否存在
        List<UserEntity> users = this.list(new QueryWrapper<UserEntity>().eq("username", loginName).or().eq("phone", loginName).or().eq("email", loginName));

        if (CollectionUtils.isEmpty(users)){
            return null;
        }

        for (UserEntity user : users) {
            // 2.查询该用户的盐，并对明文密码进行加盐加密
            password = DigestUtils.md5Hex(password + user.getSalt());
            // 3.用户输入密码和数据库密码进行比较
            if(StringUtils.equals(password,user.getPassword())){
                return user;
            }
        }

        return null;
    }

    // 发送短信验证码
    @Override
    public void sendCode() {

    }

}