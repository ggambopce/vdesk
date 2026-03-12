package com.core.vdesk.admin.supportPost.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.core.vdesk.admin.supportPost.dto.SupportAnswerRequestDto;
import com.core.vdesk.admin.supportPost.dto.SupportPostRegisterRequestDto;
import com.core.vdesk.admin.supportPost.dto.SupportPostUpdateRequestDto;
import com.core.vdesk.admin.supportPost.entity.BoardType;
import com.core.vdesk.admin.supportPost.service.SupportPostService;
import com.core.vdesk.domain.users.Users;
import com.core.vdesk.global.oauth2.PrincipalDetails;
import com.core.vdesk.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/support/posts")
public class SupportPostController {

    private final SupportPostService supportPostService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestBody SupportPostRegisterRequestDto req
    ) {
        Users actor = principal != null ? principal.getUser() : null;
        boolean isAdmin = principal != null && principal.getUser().getRoles().contains("ROLE_ADMIN");
        Long postId = supportPostService.register(actor, isAdmin, req);
        return ResponseEntity.ok(ApiResponse.ok("게시글 작성 완료", java.util.Map.of("postId", postId)));
    }

    @PatchMapping("/update/{postId}")
    public ResponseEntity<ApiResponse<?>> update(
            @AuthenticationPrincipal PrincipalDetails principal,
            @PathVariable Long postId,
            @RequestBody SupportPostUpdateRequestDto req
    ) {
        Users actor = principal != null ? principal.getUser() : null;
        boolean isAdmin = principal != null && principal.getUser().getRoles().contains("ROLE_ADMIN");
        supportPostService.update(postId, actor, isAdmin, req);
        return ResponseEntity.ok(ApiResponse.ok("게시글 수정 완료", null));
    }

    @DeleteMapping("/delete/{postId}")
    public ResponseEntity<ApiResponse<?>> delete(
            @AuthenticationPrincipal PrincipalDetails principal,
            @PathVariable Long postId
    ) {
        Users actor = principal != null ? principal.getUser() : null;
        boolean isAdmin = principal != null && principal.getUser().getRoles().contains("ROLE_ADMIN");
        supportPostService.delete(postId, actor, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("게시글 삭제 완료", null));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<?>> list(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestParam(name = "type", defaultValue = "NOTICE") BoardType type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String field,
            @RequestParam(required = false) String sort
    ) {
        Users actor = principal != null ? principal.getUser() : null;
        boolean isAdmin = principal != null && principal.getUser().getRoles().contains("ROLE_ADMIN");

        Sort s = Sort.by(Sort.Direction.DESC, "createdAt");
        if (type == BoardType.NOTICE && "PINNED".equals(sort)) {
            s = Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("createdAt"));
        } else if ("OLD".equals(sort)) {
            s = Sort.by(Sort.Direction.ASC, "createdAt");
        }

        Pageable pageable = PageRequest.of(page - 1, size, s);

        var res = supportPostService.list(type, actor, isAdmin, q, field, pageable);
        return ResponseEntity.ok(ApiResponse.ok("게시글 목록 조회 성공", res));
    }

    @GetMapping("/detail/{postId}")
    public ResponseEntity<ApiResponse<?>> detail(
            @AuthenticationPrincipal PrincipalDetails principal,
            @PathVariable Long postId
    ) {
        Users actor = principal != null ? principal.getUser() : null;
        boolean isAdmin = principal != null && principal.getUser().getRoles().contains("ROLE_ADMIN");
        var res = supportPostService.detail(postId, actor, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("게시글 상세 조회 성공", res));
    }

    @PostMapping("/answer/register/{postId}")
    public ResponseEntity<ApiResponse<?>> answerRegister(
            @PathVariable Long postId,
            @RequestBody SupportAnswerRequestDto req
    ) {
        supportPostService.answerRegister(postId, req);
        return ResponseEntity.ok(ApiResponse.ok("문의 답변 등록 완료", null));
    }

    @PatchMapping("/answer/update/{postId}")
    public ResponseEntity<ApiResponse<?>> answerUpdate(
            @PathVariable Long postId,
            @RequestBody SupportAnswerRequestDto req
    ) {
        supportPostService.answerUpdate(postId, req);
        return ResponseEntity.ok(ApiResponse.ok("문의 답변 수정 완료", null));
    }

    @DeleteMapping("/answer/delete/{postId}")
    public ResponseEntity<ApiResponse<?>> answerDelete(@PathVariable Long postId) {
        supportPostService.answerDelete(postId);
        return ResponseEntity.ok(ApiResponse.ok("문의 답변 삭제 완료", null));
    }
}
