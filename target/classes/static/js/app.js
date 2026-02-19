'use strict';

const MAX_COMPARE = 5;
let basketId = localStorage.getItem('catfood_basketId') || crypto.randomUUID();
localStorage.setItem('catfood_basketId', basketId);
let lastRecommendResult = null;
let currentDailyCalories = null;
const SORT_RANK = 'rank';
const SORT_PRICE = 'price';
const SORT_REVIEW = 'review';

document.getElementById('monthlyBudget').addEventListener('input', function () {
    const val = parseInt(this.value);
    const helper = document.getElementById('budgetHelperText');
    if (!isNaN(val) && val > 0) helper.textContent = `하루 예산 약 ${Math.round(val / 30).toLocaleString()}원`;
    else helper.textContent = '입력 후 하루 예산이 표시됩니다';
});

function updateAgeHelper() {
    const years = parseInt(document.getElementById('ageYears').value) || 0;
    const months = parseInt(document.getElementById('ageMonthsExtra').value) || 0;
    const totalMonths = years * 12 + months;
    const helper = document.getElementById('ageHelperText');
    if (totalMonths <= 0) { helper.textContent = '총 0개월 (최소 1개월 이상)'; helper.style.color = '#e74c3c'; }
    else if (totalMonths < 12) { helper.textContent = `총 ${totalMonths}개월 — 성장기 키튼 🐱`; helper.style.color = '#27ae60'; }
    else if (totalMonths < 84) {
        const y = Math.floor(totalMonths / 12), m = totalMonths % 12;
        helper.textContent = `총 ${totalMonths}개월 (${y}살 ${m}개월) — 성체 😸`;
        helper.style.color = '#27ae60';
    } else {
        const y = Math.floor(totalMonths / 12);
        helper.textContent = `총 ${totalMonths}개월 (${y}살) — 노령묘 👴`;
        helper.style.color = '#27ae60';
    }
}
document.getElementById('ageYears').addEventListener('input', updateAgeHelper);
document.getElementById('ageMonthsExtra').addEventListener('input', updateAgeHelper);

const presets = {
    'adult-neutered': { weightKg: 4.0, ageYears: 3, ageMonths: 0, gender: 'MALE', neutered: 'true', monthlyBudget: 50000 },
    'kitten': { weightKg: 1.5, ageYears: 0, ageMonths: 5, gender: 'FEMALE', neutered: 'false', monthlyBudget: 30000 },
    'senior': { weightKg: 5.0, ageYears: 10, ageMonths: 0, gender: 'FEMALE', neutered: 'true', monthlyBudget: 70000 },
};
document.querySelectorAll('.preset-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const p = presets[btn.dataset.preset];
        if (!p) return;
        document.getElementById('weightKg').value = p.weightKg;
        document.getElementById('ageYears').value = p.ageYears;
        document.getElementById('ageMonthsExtra').value = p.ageMonths;
        document.getElementById('monthlyBudget').value = p.monthlyBudget;
        document.querySelectorAll('input[name="gender"]').forEach(r => { r.checked = r.value === p.gender; });
        document.querySelectorAll('input[name="neutered"]').forEach(r => { r.checked = r.value === p.neutered; });
        updateAgeHelper();
        document.getElementById('budgetHelperText').textContent = `하루 예산 약 ${Math.round(p.monthlyBudget / 30).toLocaleString()}원`;
    });
});

document.getElementById('calorieDetailBtn').addEventListener('click', () => {
    const panel = document.getElementById('calorieDetailPanel');
    panel.classList.toggle('hidden');
    document.getElementById('calorieDetailBtn').textContent = panel.classList.contains('hidden') ? '자세히' : '접기';
});
document.getElementById('formulaToggle').addEventListener('click', () => {
    const c = document.getElementById('formulaContent');
    c.classList.toggle('hidden');
    document.getElementById('formulaToggle').textContent = c.classList.contains('hidden') ? '📐 계산식' : '📐 계산식 닫기';
});
document.getElementById('sourceToggle').addEventListener('click', () => {
    const c = document.getElementById('sourceContent');
    c.classList.toggle('hidden');
    document.getElementById('sourceToggle').textContent = c.classList.contains('hidden') ? '📋 계산 근거·출처' : '📋 닫기';
});

async function recommend() {
    const weightKg = parseFloat(document.getElementById('weightKg').value);
    const ageYears = parseInt(document.getElementById('ageYears').value) || 0;
    const ageMonthsExtra = parseInt(document.getElementById('ageMonthsExtra').value) || 0;
    const ageMonths = ageYears * 12 + ageMonthsExtra;
    const gender = document.querySelector('input[name="gender"]:checked')?.value;
    const neuteredVal = document.querySelector('input[name="neutered"]:checked')?.value;
    const monthlyBudget = parseInt(document.getElementById('monthlyBudget').value);
    const errors = [];
    if (isNaN(weightKg) || weightKg < 0.1 || weightKg > 20) errors.push('체중을 0.1~20kg 사이로 입력해주세요.');
    if (ageMonths < 1 || ageMonths > 300) errors.push('나이를 1개월 이상으로 입력해주세요.');
    if (!gender) errors.push('성별을 선택해주세요.');
    if (!neuteredVal) errors.push('중성화 여부를 선택해주세요.');
    if (isNaN(monthlyBudget) || monthlyBudget < 1000) errors.push('월 예산을 1,000원 이상으로 입력해주세요.');
    const errorDiv = document.getElementById('errorMessage');
    if (errors.length > 0) { errorDiv.textContent = errors.join(' / '); errorDiv.classList.remove('hidden'); return; }
    errorDiv.classList.add('hidden');

    const btn = document.getElementById('recommendBtn');
    btn.disabled = true;
    btn.textContent = '🔍 검색 중...';
    try {
        const res = await fetch('/api/recommend', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                weightKg, ageMonths, gender,
                neutered: neuteredVal === 'true',
                monthlyBudget,
                searchQuery: document.getElementById('searchQuery').value?.trim() || null,
            }),
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(Object.values(err).join(', ') || '서버 오류');
        }
        const data = await res.json();
        currentDailyCalories = data.dailyCalories;
        lastRecommendResult = data;
        renderResult(data);
        document.getElementById('resultSection').classList.remove('hidden');
        document.getElementById('resultSection').scrollIntoView({ behavior: 'smooth', block: 'start' });
    } catch (err) {
        errorDiv.textContent = '오류: ' + err.message;
        errorDiv.classList.remove('hidden');
    } finally {
        btn.disabled = false;
        btn.textContent = '🔍 사료 검색';
    }
}

function renderResult(data) {
    lastRecommendResult = data;
    document.getElementById('dailyCalories').textContent = Math.round(data.dailyCalories).toLocaleString();
    document.getElementById('rerCalories').textContent = Math.round(data.rerCalories).toLocaleString();
    document.getElementById('lifeFactor').textContent = data.lifeFactor.toFixed(1);
    document.getElementById('lifeStageDesc').textContent = data.lifeStageDescription;
    document.getElementById('formulaText').textContent = data.formulaDescription || '';
    document.getElementById('sourceText').textContent = data.calculationSourceDescription || '';
    document.getElementById('formulaContent').classList.add('hidden');
    document.getElementById('sourceContent').classList.add('hidden');

    const list = document.getElementById('recommendationsList');
    list.innerHTML = '';
    const noResultBox = document.getElementById('noResultBox');
    const sortSection = document.getElementById('sortSection');
    const byRank = data.recommendationsByRank || data.recommendations || [];
    const byPrice = data.recommendationsByPrice || byRank;
    const byReview = data.recommendationsByReview || byRank;
    const hasAny = byRank.length > 0;

    if (!hasAny) {
        noResultBox.classList.remove('hidden');
        document.getElementById('noResultMessage').textContent = data.reviewSortNote || '검색 결과가 없습니다. 검색어를 바꿔 보세요.';
        sortSection.classList.add('hidden');
        return;
    }
    noResultBox.classList.add('hidden');
    sortSection.classList.remove('hidden');
    document.querySelectorAll('.sort-btn').forEach(b => b.classList.toggle('active', b.dataset.sort === SORT_RANK));
    const noteEl = document.getElementById('reviewSortNote');
    noteEl.classList.add('hidden');
    noteEl.textContent = data.reviewSortNote || '';
    renderRecommendationList(byRank);
    document.querySelectorAll('.sort-btn').forEach(btn => {
        btn.onclick = () => {
            document.querySelectorAll('.sort-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            const sort = btn.dataset.sort;
            const arr = sort === SORT_PRICE ? byPrice : sort === SORT_REVIEW ? byReview : byRank;
            if (sort === SORT_REVIEW && data.reviewSortNote) { noteEl.textContent = data.reviewSortNote; noteEl.classList.remove('hidden'); }
            else noteEl.classList.add('hidden');
            renderRecommendationList(arr);
        };
    });
}

function renderRecommendationList(items) {
    const list = document.getElementById('recommendationsList');
    list.innerHTML = '';
    if (!items || items.length === 0) return;
    const compareCount = window._compareCount || 0;
    const canAdd = compareCount < MAX_COMPARE;

    items.forEach((rec, index) => {
        const rank = index + 1;
        const card = document.createElement('div');
        card.className = `recommendation-card${rank === 1 ? ' rank-1' : ''}`;
        const typeLabel = rec.type === 'DRY' ? '건식' : '습식';
        const typeBadgeClass = rec.type === 'DRY' ? 'dry' : 'wet';
        const imageBlock = rec.imageUrl ? `<div class="card-image"><img src="${escapeHtml(rec.imageUrl)}" alt="" loading="lazy"></div>` : '';
        const priceStr = rec.productPrice != null ? rec.productPrice.toLocaleString() + '원' : '—';
        const buyBlock = rec.productLink ? `<a href="${escapeHtml(rec.productLink)}" class="buy-link" target="_blank" rel="noopener">🛒 구매하기</a>` : '';
        card.innerHTML = `
            <div class="card-header">
                <div class="rank-badge ${rank === 1 ? 'gold' : ''}">${rank}</div>
                ${imageBlock}
                <div class="food-name-block">
                    ${rec.productLink ? `<a href="${escapeHtml(rec.productLink)}" class="food-name link" target="_blank" rel="noopener">${escapeHtml(rec.foodName)}</a>` : `<div class="food-name">${escapeHtml(rec.foodName)}</div>`}
                    <div class="food-brand">${escapeHtml(rec.brand || '-')}</div>
                </div>
                <span class="food-type-badge ${typeBadgeClass}">${typeLabel}</span>
            </div>
            <div class="card-body">
                <div class="stat-item"><span class="stat-label">상품가</span><span class="stat-value">${priceStr}</span></div>
            </div>
            <div class="card-footer">
                ${buyBlock}
                <button type="button" class="add-to-compare-btn" ${!canAdd ? 'disabled' : ''} data-rec='${escapeHtml(JSON.stringify({ productLink: rec.productLink, productName: rec.foodName, brand: rec.brand, imageUrl: rec.imageUrl, lprice: rec.productPrice }))}'>비교 목록에 추가</button>
            </div>`;
        list.appendChild(card);
    });
    list.querySelectorAll('.add-to-compare-btn').forEach(b => {
        b.addEventListener('click', function () {
            try {
                const rec = JSON.parse(this.dataset.rec);
                addToCompare(rec);
            } catch (_) {}
        });
    });
}

function escapeHtml(s) {
    if (s == null) return '';
    const div = document.createElement('div');
    div.textContent = s;
    return div.innerHTML;
}

async function addToCompare(rec) {
    try {
        const res = await fetch(`/api/compare/add?basketId=${encodeURIComponent(basketId)}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                productLink: rec.productLink,
                productName: rec.productName,
                brand: rec.brand || '',
                imageUrl: rec.imageUrl || '',
                lprice: rec.lprice,
            }),
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            alert(err.error || '추가 실패');
            return;
        }
        await loadCompareList();
    } catch (e) {
        alert('네트워크 오류');
    }
}

async function loadCompareList() {
    const daily = currentDailyCalories != null ? currentDailyCalories : 300;
    const res = await fetch(`/api/compare?basketId=${encodeURIComponent(basketId)}&dailyCalories=${daily}`);
    const items = await res.json();
    window._compareCount = items.length;
    const wrap = document.getElementById('compareList');
    wrap.innerHTML = '';
    if (items.length === 0) {
        wrap.innerHTML = '<p class="card-no-data">추천 결과에서 "비교 목록에 추가"를 누르면 여기에 쌓입니다. (최대 5개)</p>';
        return;
    }
    items.forEach(item => {
        const slot = document.createElement('div');
        slot.className = 'compare-slot' + (item.kcalPer100g && item.weightKg ? ' filled' : '');
        const hasInfo = item.proteinPercent != null || item.fatPercent != null || item.kcalPer100g != null || item.price != null || item.weightKg != null;
        slot.innerHTML = `
            <div class="slot-info">
                ${item.imageUrl ? `<img class="slot-image" src="${escapeHtml(item.imageUrl)}" alt="">` : ''}
                <div class="slot-name">${escapeHtml(item.productName)}</div>
                ${item.productLink ? `<a href="${escapeHtml(item.productLink)}" target="_blank" rel="noopener">상품 보기</a>` : ''}
            </div>
            <div class="slot-form">
                <label>단백질(%)</label><input type="number" step="0.1" placeholder="포장지 확인" data-field="proteinPercent" value="${item.proteinPercent ?? ''}">
                <label>지방(%)</label><input type="number" step="0.1" placeholder="포장지 확인" data-field="fatPercent" value="${item.fatPercent ?? ''}">
                <label>100g당 칼로리</label><input type="number" step="1" placeholder="kcal" data-field="kcalPer100g" value="${item.kcalPer100g ?? ''}">
                <label>가격(원)</label><input type="number" placeholder="실제 구매가" data-field="price" value="${item.price ?? item.lprice ?? ''}">
                <label>용량(kg)</label><input type="number" step="0.001" placeholder="예: 2" data-field="weightKg" value="${item.weightKg ?? ''}">
                <div class="slot-save-wrap"><button type="button" class="slot-save">저장</button></div>
            </div>
            <div class="slot-computed">${item.dailyAmountGrams != null ? `하루 급여량: ${item.dailyAmountGrams}g` : ''} ${item.dailyCost != null ? `· 하루 비용: ${item.dailyCost.toLocaleString()}원` : ''} ${item.monthlyCost != null ? `· 월: ${item.monthlyCost.toLocaleString()}원` : ''}</div>
            <button type="button" class="slot-remove" data-id="${escapeHtml(item.id)}">삭제</button>`;
        wrap.appendChild(slot);
        slot.querySelector('.slot-save').addEventListener('click', () => saveCompareItem(item.id, slot));
        slot.querySelector('.slot-remove').addEventListener('click', () => removeCompareItem(item.id));
    });
    renderCompareTable(items);
    if (lastRecommendResult && lastRecommendResult.recommendationsByRank) {
        renderRecommendationList(document.querySelector('.sort-btn.active')?.dataset.sort === SORT_PRICE ? lastRecommendResult.recommendationsByPrice : document.querySelector('.sort-btn.active')?.dataset.sort === SORT_REVIEW ? lastRecommendResult.recommendationsByReview : lastRecommendResult.recommendationsByRank);
    }
}

function saveCompareItem(itemId, slotEl) {
    const payload = {};
    slotEl.querySelectorAll('[data-field]').forEach(inp => {
        const v = inp.value?.trim();
        const f = inp.dataset.field;
        if (v === '') return;
        if (f === 'proteinPercent' || f === 'fatPercent' || f === 'kcalPer100g' || f === 'weightKg') payload[f] = parseFloat(v);
        else if (f === 'price') payload[f] = parseInt(v);
    });
    fetch(`/api/compare/${encodeURIComponent(itemId)}?basketId=${encodeURIComponent(basketId)}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
    }).then(() => loadCompareList());
}

async function removeCompareItem(itemId) {
    await fetch(`/api/compare/${encodeURIComponent(itemId)}?basketId=${encodeURIComponent(basketId)}`, { method: 'DELETE' });
    loadCompareList();
}

function renderCompareTable(items) {
    const filled = items.filter(i => i.kcalPer100g != null && i.weightKg != null && (i.price != null || i.lprice != null));
    const wrap = document.getElementById('compareTableWrap');
    const table = document.getElementById('compareTable');
    if (filled.length < 2) {
        wrap.classList.add('hidden');
        return;
    }
    wrap.classList.remove('hidden');
    const headers = ['제품명', '단백질(%)', '지방(%)', '가격(원)', '하루 급여량(g)', '하루 비용(원)', '월 비용(원)'];
    let html = '<thead><tr>' + headers.map(h => `<th>${h}</th>`).join('') + '</tr></thead><tbody>';
    filled.forEach(item => {
        const firstWord = (item.productName || '').trim().split(/\s+/)[0] || item.productName || '—';
        html += '<tr>';
        html += `<td title="${escapeHtml(item.productName || '')}">${escapeHtml(firstWord)}</td>`;
        html += `<td>${item.proteinPercent != null ? item.proteinPercent : '—'}</td>`;
        html += `<td>${item.fatPercent != null ? item.fatPercent : '—'}</td>`;
        html += `<td>${(item.price != null ? item.price : item.lprice)?.toLocaleString() ?? '—'}</td>`;
        html += `<td>${item.dailyAmountGrams != null ? item.dailyAmountGrams : '—'}</td>`;
        html += `<td>${item.dailyCost != null ? item.dailyCost.toLocaleString() : '—'}</td>`;
        html += `<td>${item.monthlyCost != null ? item.monthlyCost.toLocaleString() : '—'}</td>`;
        html += '</tr>';
    });
    html += '</tbody>';
    table.innerHTML = html;
}

(async function init() {
    try {
        const r = await fetch('/api/real-search-available');
        const d = await r.json();
        if (!d.available) document.getElementById('apiUnavailableNote').classList.remove('hidden');
    } catch (_) {}
    loadCompareList();
})();

document.getElementById('recommendBtn').addEventListener('click', recommend);
document.addEventListener('keydown', e => {
    if (e.key === 'Enter' && !e.shiftKey && document.activeElement?.tagName === 'INPUT') {
        e.preventDefault();
        recommend();
    }
});
