$(document).ready(function () {

  // ── Lenis 부드러운 스크롤 ────────────────────────────────
  if (typeof Lenis !== 'undefined') {
    const lenis = new Lenis({
      duration: 1.2,
      easing: (t) => Math.min(1, 1.001 - Math.pow(2, -10 * t)),
      smooth: true,
    });
    function raf(time) { lenis.raf(time); requestAnimationFrame(raf); }
    requestAnimationFrame(raf);
  }

  // ── 로고 슬라이더 (Slick) ────────────────────────────────
  if ($('.trust-logos-slider').length) {
    $('.trust-logos-slider').slick({
      infinite: true,
      slidesToShow: 5,
      slidesToScroll: 1,
      autoplay: true,
      autoplaySpeed: 3000,
      speed: 1000,
      arrows: false,
      dots: false,
      responsive: [
        { breakpoint: 1200, settings: { slidesToShow: 3 } },
        { breakpoint: 768,  settings: { slidesToShow: 2 } },
        { breakpoint: 480,  settings: { slidesToShow: 1 } },
      ],
    });
  }

  // ── 스크롤 기반 헤더 애니메이션 ──────────────────────────
  let lastScroll = 0;
  $(window).on('scroll', function () {
    const st = $(this).scrollTop();
    const $wrap = $('#topWrap');

    // 스크롤 시 배경색 추가
    if (st > 0) {
      $wrap.addClass('scrolled');
    } else {
      $wrap.removeClass('scrolled');
    }

    // 아래 스크롤 100px 이상 → 헤더 숨김 / 위로 → 다시 표시
    if (st > lastScroll && st > 100) {
      $wrap.addClass('hidden');
    } else {
      $wrap.removeClass('hidden');
    }
    lastScroll = st;
  });

  // ── 요금제 탭 전환 ───────────────────────────────────────
  $('.tab-btn').on('click', function () {
    const idx = $(this).index();
    $('.tab-btn').removeClass('active');
    $(this).addClass('active');
    $('.tab-pane').removeClass('active');
    $('.tab-pane').eq(idx).addClass('active');
  });

  // ── 히어로 단어 페이드 애니메이션 ───────────────────────
  const words = ['#fade-word-1', '#fade-word-2', '#fade-word-3'];
  let wIdx = 0;
  function showNextWord() {
    words.forEach(w => $(w).css('opacity', '0'));
    $(words[wIdx]).css('opacity', '1');
    wIdx = (wIdx + 1) % words.length;
  }
  showNextWord();
  setInterval(showNextWord, 3000);

  // ── 버튼 radial-gradient 호버 효과 ──────────────────────
  function initRadialHover() {
    $('.btn-radial').each(function () {
      const $btn = $(this);
      $btn.on('mouseenter', function (e) {
        const r = this.getBoundingClientRect();
        const size = Math.min(Math.max(r.width, r.height) * 0.8, 200);
        $btn.css({ '--bx': (e.clientX - r.left) + 'px', '--by': (e.clientY - r.top) + 'px', '--bs': '0px' });
        setTimeout(() => $btn.css('--bs', size + 'px'), 10);
      });
      $btn.on('mousemove', function (e) {
        const r = this.getBoundingClientRect();
        $btn.css({ '--bx': (e.clientX - r.left) + 'px', '--by': (e.clientY - r.top) + 'px' });
      });
      $btn.on('mouseleave', function () { $btn.css('--bs', '0px'); });
    });
  }
  initRadialHover();

  // ── 다크모드 토글 ────────────────────────────────────────
  $('#darkModeBtn').on('click', function () {
    $(this).toggleClass('active');
    $('html').toggleClass('dark-theme');
  });

  // ── 뉴스레터 폼 제출 ─────────────────────────────────────
  $('#newsletterForm').on('submit', function (e) {
    e.preventDefault();

    // reCAPTCHA 검증
    if (typeof grecaptcha !== 'undefined') {
      const res = grecaptcha.getResponse();
      if (!res) { alert('reCAPTCHA를 완료해주세요.'); return; }
    }

    const email = $('#newsletterEmail').val().trim();
    if (!email) { alert('이메일을 입력해주세요.'); return; }
    const emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRe.test(email)) { alert('올바른 이메일 형식을 입력해주세요.'); return; }

    // TODO: 실제 구독 API 연동
    $.post('/api/newsletter/subscribe', { email }, function () {
      alert('구독이 완료되었습니다!');
      $('#newsletterEmail').val('');
      if (typeof grecaptcha !== 'undefined') grecaptcha.reset();
    }).fail(function () {
      alert('구독 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    });
  });

  // reCAPTCHA 전역 콜백
  window.onRecaptchaSuccess  = function () {};
  window.onRecaptchaExpired  = function () {};
  window.onRecaptchaError    = function () {};
});
