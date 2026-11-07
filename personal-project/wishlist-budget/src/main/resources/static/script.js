
'use strict';

const API = '/collections';
const PAGE_SIZE = 4;

let items = [];
let currentPage = 1;

const $ = (id) => document.getElementById(id);
const money = (price) => `${Number(price).toLocaleString('ko-KR')}원`;

// 상품 종류와 관계없이 공통 SF 아이콘 사용
function iconFor() {
  return '◇';
}

function itemCard(item) {
  const card = document.createElement('article');
  card.className = 'item';

  const icon = document.createElement('span');
  icon.className = 'item-icon';
  icon.textContent = iconFor();
  icon.setAttribute('aria-hidden', 'true');

  const info = document.createElement('div');
  info.className = 'item-info';

  const name = document.createElement('div');
  name.className = 'item-name';
  name.textContent = item.name;
  name.title = item.name;

  const id = document.createElement('div');
  id.className = 'item-id';
  id.textContent = `ITEM #${item.id}`;

  info.append(name, id);

  const price = document.createElement('span');
  price.className = 'item-price';
  price.textContent = money(item.price);

  card.append(icon, info, price);

  return card;
}

function message(container, text) {
  container.replaceChildren();

  const empty = document.createElement('div');
  empty.className = 'empty';
  empty.textContent = text;

  container.append(empty);
}

function showTab(tab) {
  document.querySelectorAll('.tab').forEach((button) => {
    const selected = button.dataset.tab === tab;

    button.classList.toggle('active', selected);
    button.setAttribute('aria-selected', String(selected));
  });

  document.querySelectorAll('.screen').forEach((screen) => {
    const selected = screen.id === `screen-${tab}`;

    screen.hidden = !selected;
    screen.classList.toggle('active', selected);
  });
}

function renderList() {
  const container = $('item-list');

  const pages = Math.max(1, Math.ceil(items.length / PAGE_SIZE));

  currentPage = Math.min(Math.max(currentPage, 1), pages);

  $('item-count').textContent = items.length;
  $('page-label').textContent = `${currentPage} / ${pages}`;

  $('prev-btn').disabled = currentPage === 1;
  $('next-btn').disabled = currentPage === pages;

  if (!items.length) {
    message(
      container,
      '아직 등록한 아이템이 없어요. 아이템 등록 탭에서 추가해 보세요.'
    );
    return;
  }

  const start = (currentPage - 1) * PAGE_SIZE;

  container.replaceChildren(
    ...items.slice(start, start + PAGE_SIZE).map(itemCard)
  );
}

async function request(url, options) {
  let response;

  try {
    response = await fetch(url, options);
  } catch {
    throw new Error(
      '서버에 연결할 수 없어요. Spring Boot 실행 상태를 확인해 주세요.'
    );
  }

  if (!response.ok) {
    throw new Error(
      response.status === 400
        ? '입력값을 확인해 주세요.'
        : `요청에 실패했어요. (HTTP ${response.status})`
    );
  }

  return response.status === 204 ? null : response.json();
}

async function loadItems() {
  try {
    const result = await request(API);

    if (!Array.isArray(result)) {
      throw new Error('목록 응답 형식이 올바르지 않아요.');
    }

    items = result;
    renderList();

  } catch (error) {
    message($('item-list'), error.message);
  }
}

// 탭 전환
document.querySelectorAll('.tab').forEach((button) => {
  button.addEventListener('click', () => {
    showTab(button.dataset.tab);
  });
});

// 목록 페이지 이동
$('prev-btn').addEventListener('click', () => {
  currentPage--;
  renderList();
});

$('next-btn').addEventListener('click', () => {
  currentPage++;
  renderList();
});

// 목록 새로고침
$('refresh-btn').addEventListener('click', loadItems);

// 아이템 등록
$('add-form').addEventListener('submit', async (event) => {
  event.preventDefault();

  const name = $('item-name').value.trim();
  const price = Number($('item-price').value);
  const notice = $('add-message');

  notice.classList.remove('error');

  if (
    !name ||
    !Number.isInteger(price) ||
    price < 0 ||
    price > 2147483647
  ) {
    notice.textContent =
      '이름과 0원 이상의 올바른 가격을 입력해 주세요.';
    notice.classList.add('error');
    return;
  }

  $('add-btn').disabled = true;

  try {
    await request(API, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ name, price })
    });

    $('add-form').reset();

    notice.textContent = '등록 완료! 위시리스트로 이동합니다.';

    currentPage = 1;

    await loadItems();

    showTab('list');

  } catch (error) {
    notice.textContent = error.message;
    notice.classList.add('error');

  } finally {
    $('add-btn').disabled = false;
  }
});

// 예산별 추천
$('budget-form').addEventListener('submit', async (event) => {
  event.preventDefault();

  const budget = Number($('budget').value);

  if (
    !Number.isInteger(budget) ||
    budget < 0 ||
    budget > 2147483647
  ) {
    message(
      $('recommend-results'),
      '0원 이상의 올바른 예산을 입력해 주세요.'
    );
    return;
  }

  $('recommend-btn').disabled = true;
  $('result-count').textContent = '검색 중';

  try {
    const result = await request(
      `${API}/recommend?budget=${encodeURIComponent(budget)}`
    );

    if (!Array.isArray(result)) {
      throw new Error('추천 응답 형식이 올바르지 않아요.');
    }

    $('result-count').textContent = `${result.length}개 추천`;

    if (!result.length) {
      message(
        $('recommend-results'),
        '예산에 맞는 아이템이 없어요.'
      );
    } else {
      $('recommend-results').replaceChildren(
        ...result.map(itemCard)
      );
    }

  } catch (error) {
    $('result-count').textContent = '오류';
    message($('recommend-results'), error.message);

  } finally {
    $('recommend-btn').disabled = false;
  }
});

// 최초 목록 조회
loadItems();
