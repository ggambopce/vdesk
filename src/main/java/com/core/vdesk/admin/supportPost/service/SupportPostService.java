package com.core.vdesk.admin.supportPost.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.core.vdesk.admin.supportPost.dto.SupportAnswerRequestDto;
import com.core.vdesk.admin.supportPost.dto.SupportPostDetailResponseDto;
import com.core.vdesk.admin.supportPost.dto.SupportPostListItemResponseDto;
import com.core.vdesk.admin.supportPost.dto.SupportPostListResponseDto;
import com.core.vdesk.admin.supportPost.dto.SupportPostRegisterRequestDto;
import com.core.vdesk.admin.supportPost.dto.SupportPostUpdateRequestDto;
import com.core.vdesk.admin.supportPost.entity.BoardType;
import com.core.vdesk.admin.supportPost.entity.InquiryDetails;
import com.core.vdesk.admin.supportPost.entity.PartnershipDetails;
import com.core.vdesk.admin.supportPost.entity.PostStatus;
import com.core.vdesk.admin.supportPost.entity.SupportPosts;
import com.core.vdesk.admin.supportPost.entity.Visibility;
import com.core.vdesk.admin.supportPost.repository.InquiryDetailRepository;
import com.core.vdesk.admin.supportPost.repository.PartnershipDetailRepository;
import com.core.vdesk.admin.supportPost.repository.SupportPostRepository;
import com.core.vdesk.domain.users.Users;
import com.core.vdesk.global.exception.BusinessException;
import com.core.vdesk.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupportPostService {
    private final SupportPostRepository postRepo;
    private final InquiryDetailRepository inquiryRepo;
    private final PartnershipDetailRepository partnershipRepo;

    @Transactional
    public Long register(Users actor, boolean isAdmin, SupportPostRegisterRequestDto req) {
        // 타입별 작성 권한
        if (req.getType() == BoardType.NOTICE && !isAdmin) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "NOTICE 작성은 관리자만 가능");
        }
        if ((req.getType() == BoardType.INQUIRY || req.getType() == BoardType.PARTNERSHIP) && actor == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인 필요");
        }

        // 문의/제휴는 기본 비공개 강제 정책(원하면)
        if (req.getType() != BoardType.NOTICE) {
            req.setVisibility(Visibility.PRIVATE);
            req.setPinned(false);
        }

        SupportPosts p = new SupportPosts();
        p.setType(req.getType());
        p.setTitle(req.getTitle());
        p.setContent(req.getContent());
        p.setVisibility(req.getVisibility());
        p.setPinned(req.isPinned());
        p.setAuthor(actor); // NOTICE도 작성자는 관리자 계정으로
        postRepo.save(p);

        if (req.getType() == BoardType.INQUIRY) {
            if (req.getInquiry() == null) throw new BusinessException(ErrorCode.INVALID_REQUEST, "inquiry 필드 필요");
            InquiryDetails d = new InquiryDetails();
            d.setPost(p);
            d.setEmail(req.getInquiry().getEmail());
            d.setPhone(req.getInquiry().getPhone());
            d.setCategory(req.getInquiry().getCategory());
            inquiryRepo.save(d);
        }

        if (req.getType() == BoardType.PARTNERSHIP) {
            if (req.getPartnership() == null) throw new BusinessException(ErrorCode.INVALID_REQUEST, "partnership 필드 필요");
            PartnershipDetails d = new PartnershipDetails();
            d.setPost(p);
            d.setCompanyName(req.getPartnership().getCompanyName());
            d.setContactName(req.getPartnership().getContactName());
            d.setEmail(req.getPartnership().getEmail());
            d.setPhone(req.getPartnership().getPhone());
            d.setProposal(req.getPartnership().getProposal());
            partnershipRepo.save(d);
        }

        return p.getId();
    }

    @Transactional
    public void update(Long postId, Users actor, boolean isAdmin, SupportPostUpdateRequestDto req) {
        SupportPosts p = postRepo.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "게시글 없음"));

        // 타입별 수정 권한
        if (p.getType() == BoardType.NOTICE) {
            if (!isAdmin) throw new BusinessException(ErrorCode.FORBIDDEN, "NOTICE 수정은 관리자만 가능");
        } else {
            assertOwnerOrAdmin(p, actor, isAdmin);
            // 답변 완료 후 수정 불가 정책(선택)
            if (p.getStatus() == PostStatus.ANSWERED) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "답변 완료 후 수정 불가");
            }
        }

        p.setTitle(req.getTitle());
        p.setContent(req.getContent());

        if (req.getVisibility() != null) {
            // 문의/제휴는 비공개 강제 정책이면 막기
            if (p.getType() != BoardType.NOTICE) {
                p.setVisibility(Visibility.PRIVATE);
            } else {
                p.setVisibility(req.getVisibility());
            }
        }
        if (req.getPinned() != null) {
            if (p.getType() == BoardType.NOTICE) p.setPinned(req.getPinned());
        }
    }

    @Transactional
    public void delete(Long postId, Users actor, boolean isAdmin) {
        SupportPosts p = postRepo.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "게시글 없음"));

        if (p.getType() == BoardType.NOTICE) {
            if (!isAdmin) throw new BusinessException(ErrorCode.FORBIDDEN, "NOTICE 삭제는 관리자만 가능");
        } else {
            assertOwnerOrAdmin(p, actor, isAdmin);
        }

        p.setDeleted(true);
    }

    @Transactional(readOnly = true)
    public SupportPostListResponseDto list(
            BoardType type,
            Users actor,
            boolean isAdmin,
            String q,
            String field,
            Pageable pageable
    ) {
        Page<SupportPosts> pageResult;

        if (type == BoardType.NOTICE) {
            pageResult = postRepo.search(type, q, normField(field), pageable);
        } else {
            if (actor == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인 필요");
            if (isAdmin) {
                pageResult = postRepo.search(type, q, normField(field), pageable);
            } else {
                pageResult = postRepo.searchMine(type, actor.getUserId(), q, normField(field), pageable);
            }
        }

        return new SupportPostListResponseDto(
                pageable.getPageNumber() + 1,
                pageable.getPageSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.getContent().stream()
                        .map(SupportPostListItemResponseDto::from)
                        .toList()
        );
    }


    @Transactional
    public SupportPostDetailResponseDto detail(Long postId, Users actor, boolean isAdmin) {
        SupportPosts p = postRepo.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "게시글 없음"));

        // 조회 권한
        if (p.getType() == BoardType.NOTICE) {
            // permitAll
        } else {
            if (actor == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인 필요");
            assertOwnerOrAdmin(p, actor, isAdmin);
        }

        // viewCount 증가 (NOTICE만 올릴지 정책 선택)
        p.setViewCount(p.getViewCount() + 1);

        InquiryDetails inquiry = null;
        PartnershipDetails partnership = null;

        if (p.getType() == BoardType.INQUIRY) {
            inquiry = inquiryRepo.findById(p.getId()).orElse(null);
        } else if (p.getType() == BoardType.PARTNERSHIP) {
            partnership = partnershipRepo.findById(p.getId()).orElse(null);
        }

        return SupportPostDetailResponseDto.from(p, inquiry, partnership);
    }

    @Transactional
    public void answerRegister(Long postId, SupportAnswerRequestDto req) {
        SupportPosts p = postRepo.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "게시글 없음"));

        if (p.getType() == BoardType.NOTICE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "NOTICE는 답변 대상 아님");
        }
        if (p.getStatus() == PostStatus.ANSWERED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 답변 존재");
        }

        if (p.getType() == BoardType.INQUIRY) {
            InquiryDetails d = inquiryRepo.findById(p.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "상세 없음"));
            d.setAnswer(req.getAnswer());
            d.setAnsweredAt(Instant.now());
        } else {
            PartnershipDetails d = partnershipRepo.findById(p.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "상세 없음"));
            d.setAnswer(req.getAnswer());
            d.setAnsweredAt(Instant.now());
        }

        p.markAnswered();

        // notify=true면 이메일/알림 발송 (이벤트로 빼는 걸 추천)
    }

    @Transactional
    public void answerUpdate(Long postId, SupportAnswerRequestDto req) {
        SupportPosts p = postRepo.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "게시글 없음"));

        if (p.getType() == BoardType.NOTICE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "NOTICE는 답변 대상 아님");
        }
        if (p.getStatus() != PostStatus.ANSWERED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "답변이 없음");
        }

        if (p.getType() == BoardType.INQUIRY) {
            InquiryDetails d = inquiryRepo.findById(p.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "상세 없음"));
            d.setAnswer(req.getAnswer());
            d.setAnsweredAt(Instant.now());
        } else {
            PartnershipDetails d = partnershipRepo.findById(p.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "상세 없음"));
            d.setAnswer(req.getAnswer());
            d.setAnsweredAt(Instant.now());
        }
    }

    @Transactional
    public void answerDelete(Long postId) {
        SupportPosts p = postRepo.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "게시글 없음"));

        if (p.getType() == BoardType.NOTICE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "NOTICE는 답변 대상 아님");
        }

        if (p.getType() == BoardType.INQUIRY) {
            InquiryDetails d = inquiryRepo.findById(p.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "상세 없음"));
            d.setAnswer(null);
            d.setAnsweredAt(null);
        } else {
            PartnershipDetails d = partnershipRepo.findById(p.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "상세 없음"));
            d.setAnswer(null);
            d.setAnsweredAt(null);
        }

        p.markOpen();
    }

    private void assertOwnerOrAdmin(SupportPosts p, Users actor, boolean isAdmin) {
        if (isAdmin) return;
        if (actor == null || !p.getAuthor().getUserId().equals(actor.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인 글만 접근 가능");
        }
    }

    private String normField(String field) {
        if (field == null) return "ALL";
        return switch (field) {
            case "TITLE", "CONTENT", "ALL" -> field;
            default -> "ALL";
        };
    }
}
