package com.gong.url.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.gong.url.Util;
import com.gong.url.bo.UrlBO;
import com.gong.url.dto.UrlDTO;
import com.gong.url.request.UrlRequest;
import com.gong.url.serice.UrlService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Url控制器
 *
 * @author gongjunbing
 * @date 2020/03/12 23:07
 **/
@RestController
@AllArgsConstructor
@Api("短链接接口")
@Validated
public class UrlController {

    private UrlService urlService;

    @ApiOperationSupport(author = "tiansheng9527@gmail.com")
    @ApiOperation("生成短链接")
    @PostMapping("/generic")
    public ResponseEntity<UrlDTO> generic(@RequestBody @Valid UrlRequest request) {
        UrlBO urlBO = new UrlBO();
        BeanUtils.copyProperties(request, urlBO);

        int id = urlService.insertUrl(urlBO);

        String key = Util.convertToBase62(id);

        urlBO.setExpireTime(LocalDateTime.now().plusDays(7));
        urlBO.setIsDelete(false);
        urlBO.setShortKey(key);
        int result = urlService.updateUrl(id, urlBO);

        UrlDTO response = new UrlDTO();
        BeanUtils.copyProperties(urlBO, response);

        return ResponseEntity.ok(response);
    }

    @ApiOperationSupport(author = "tiansheng9527@gmail.com")
    @ApiOperation("生成短链接")
    @PostMapping("/genericForm")
    @ApiImplicitParams(@ApiImplicitParam(name = "originUrl", paramType = "query"))
    public ResponseEntity<UrlDTO> genericForm(@Valid @NotBlank @URL @RequestParam("originUrl") String originUrl) {
        UrlBO urlBO = new UrlBO();
        urlBO.setOriginUrl(originUrl);

        int id = urlService.insertUrl(urlBO);

        String key = Util.convertToBase62(id);

        urlBO.setExpireTime(LocalDateTime.now().plusDays(7));
        urlBO.setIsDelete(false);
        urlBO.setShortKey(key);
        int result = urlService.updateUrl(id, urlBO);

        UrlDTO response = new UrlDTO();
        BeanUtils.copyProperties(urlBO, response);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/u/{key}")
    public void redirect(@PathVariable("key") @Valid String key, HttpServletResponse response) {
        if (StringUtils.isEmpty(key)) {
            return;
        }

        UrlBO urlBO = urlService.selectByShortKey(key);
        if (urlBO == null) {
            return;
        }
        if (urlBO.getIsDelete()) {
            return;
        }
        if (urlBO.getExpireTime().compareTo(LocalDateTime.now()) < 0) {
            return;
        }
        String originUrl = urlBO.getOriginUrl();
        if (StringUtils.isEmpty(originUrl)) {
            return;
        }
        try {
            response.sendRedirect(originUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
