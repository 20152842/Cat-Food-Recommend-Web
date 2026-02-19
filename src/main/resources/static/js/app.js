'use strict';

// ===== 예산 helper 텍스트 실시간 업데이트 =====
document.getElementById('monthlyBudget').addEventListener('input', function () {
    const val = parseInt(this.value);
    const helper = document.getElementById('budgetHelperText');
    if (!isNaN(val) && val > 0) {
        const daily = Math.round(val / 30);
        helper.textContent = `하루 예산 약 ${daily.toLocaleString()}원`;
    } else {
        helper.textContent = '입력 후 하루 예산이 표시됩니다';
    }
});

// ===== 나이 helper 텍스트 실시간 업데이트 =====
function updateAgeHelper() {
    const years = parseInt(document.getElementById('ageYears').value) || 0;
    const months = parseInt(document.getElementById('ageMonthsExtra').value) || 0;
    const totalMonths = years * 12 + months;
    const helper = document.getElementById('ageHelperText');

    if (totalMonths <= 0) {
        helper.textContent = '총 0개월 (최소 1개월 이상)';
        helper.style.color = '#e74c3c';
    } else if (totalMonths < 12) {
        helper.textContent = `총 ${totalMonths}개월 — 성장기 키튼 🐱`;
        helper.style.color = '#27ae60';
    } else if (totalMonths < 84) {
        const y = Math.floor(totalMonths / 12);
        const m = totalMonths % 12;
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

// ===== 예시 입력 버튼 =====
const presets = {
    'adult-neutered': { weightKg: 4.0, ageYears: 3, ageMonths: 0, gender: 'MALE', neutered: 'true', monthlyBudget: 50000 },
    'kitten':          { weightKg: 1.5, ageYears: 0, ageMonths: 5, gender: 'FEMALE', neutered: 'false', monthlyBudget: 30000 },
    'senior':          { weightKg: 5.0, ageYears: 10, ageMonths: 0, gender: 'FEMALE', neutered: 'true', monthlyBudget: 70000 },
};

document.querySelectorAll('.preset-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const preset = presets[btn.dataset.preset];
        if (!preset) return;

        document.getElementById('weightKg').value = preset.weightKg;
        document.getElementById('ageYears').value = preset.ageYears;
        document.getElementById('ageMonthsExtra').value = preset.ageMonths;
        document.getElementById('monthlyBudget').value = preset.monthlyBudget;

        document.querySelectorAll('input[name="gender"]').forEach(r => {
            r.checked = r.value === preset.gender;
        });
        document.querySelectorAll('input[name="neutered"]').forEach(r => {
            r.checked = r.value === preset.neutered;
        });

        updateAgeHelper();
        document.getElementById('budgetHelperText').textContent =
            `하루 예산 약 ${Math.round(preset.monthlyBudget / 30).toLocaleString()}원`;
    });
});

// ===== 계산식 토글 =====
document.getElementById('formulaToggle').addEventListener('click', () => {
    const content = document.getElementById('formulaContent');
    const btn = document.getElementById('formulaToggle');
    content.classList.toggle('hidden');
    btn.textContent = content.classList.contains('hidden') ? '📐 계산식 보기' : '📐 계산식 닫기';
});

// ===== 추천 요청 =====
async function recommend() {
    const weightKg = parseFloat(document.getElementById('weightKg').value);
    const ageYears = parseInt(document.getElementById('ageYears').value) || 0;
    const ageMonthsExtra = parseInt(document.getElementById('ageMonthsExtra').value) || 0;
    const ageMonths = ageYears * 12 + ageMonthsExtra;
    const gender = document.querySelector('input[name="gender"]:checked')?.value;
    const neuteredVal = document.querySelector('input[name="neutered"]:checked')?.value;
    const monthlyBudget = parseInt(document.getElementById('monthlyBudget').value);

    // 입력 검증
    const errors = [];
    if (isNaN(weightKg) || weightKg < 0.1 || weightKg > 20) errors.push('체중을 0.1~20kg 사이로 입력해주세요.');
    if (ageMonths < 1 || ageMonths > 300) errors.push('나이를 1개월 이상으로 입력해주세요.');
    if (!gender) errors.push('성별을 선택해주세요.');
    if (!neuteredVal) errors.push('중성화 여부를 선택해주세요.');
    if (isNaN(monthlyBudget) || monthlyBudget < 1000) errors.push('월 예산을 1,000원 이상으로 입력해주세요.');

    const errorDiv = document.getElementById('errorMessage');
    if (errors.length > 0) {
        errorDiv.textContent = errors.join(' / ');
        errorDiv.classList.remove('hidden');
        return;
    }
    errorDiv.classList.add('hidden');

    const btn = document.getElementById('recommendBtn');
    btn.disabled = true;
    btn.textContent = '🔍 추천 중...';

    try {
        const response = await fetch('/api/recommend', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                weightKg,
                ageMonths,
                gender,
                neutered: neuteredVal === 'true',
                monthlyBudget,
            }),
        });

        if (!response.ok) {
            const errData = await response.json().catch(() => ({}));
            const msg = Object.values(errData).join(', ') || `서버 오류 (${response.status})`;
            throw new Error(msg);
        }

        const data = await response.json();
        renderResult(data);

        document.getElementById('resultSection').classList.remove('hidden');
        document.getElementById('resultSection').scrollIntoView({ behavior: 'smooth', block: 'start' });

    } catch (err) {
        errorDiv.textContent = '오류: ' + err.message;
        errorDiv.classList.remove('hidden');
    } finally {
        btn.disabled = false;
        btn.textContent = '🔍 사료 추천받기';
    }
}

// ===== 결과 렌더링 =====
function renderResult(data) {
    document.getElementById('dailyCalories').textContent = Math.round(data.dailyCalories).toLocaleString();
    document.getElementById('rerCalories').textContent = Math.round(data.rerCalories).toLocaleString();
    document.getElementById('lifeFactor').textContent = data.lifeFactor.toFixed(1);
    document.getElementById('lifeStageDesc').textContent = data.lifeStageDescription;
    document.getElementById('formulaText').textContent = data.formulaDescription;

    // 계산식 다시 닫기
    document.getElementById('formulaContent').classList.add('hidden');
    document.getElementById('formulaToggle').textContent = '📐 계산식 보기';

    const list = document.getElementById('recommendationsList');
    list.innerHTML = '';

    const noResultBox = document.getElementById('noResultBox');

    if (!data.recommendations || data.recommendations.length === 0) {
        noResultBox.classList.remove('hidden');
        return;
    }
    noResultBox.classList.add('hidden');

    data.recommendations.forEach(rec => {
        const card = document.createElement('div');
        card.className = `recommendation-card${rec.rank === 1 ? ' rank-1' : ''}`;

        const typeBadgeClass = rec.type === 'DRY' ? 'dry' : 'wet';
        const typeLabel = rec.type === 'DRY' ? '건식' : '습식';
        const rankBadgeClass = rec.rank === 1 ? 'gold' : '';

        const proteinBarWidth = Math.min(rec.proteinPercent * 2, 100);
        const fatBarWidth = Math.min(rec.fatPercent * 5, 100);

        card.innerHTML = `
            <div class="card-header">
                <div class="rank-badge ${rankBadgeClass}">${rec.rank}</div>
                <div class="food-name-block">
                    <div class="food-name">${rec.foodName}</div>
                    <div class="food-brand">${rec.brand}</div>
                </div>
                <span class="food-type-badge ${typeBadgeClass}">${typeLabel}</span>
            </div>

            <div class="card-body">
                <div class="stat-item">
                    <span class="stat-label">하루 급여량</span>
                    <span class="stat-value highlight">${rec.dailyAmountGrams.toFixed(0)}<small>g</small></span>
                </div>
                <div class="stat-item">
                    <span class="stat-label">하루 비용</span>
                    <span class="stat-value">${rec.dailyCost.toLocaleString()}<small>원</small></span>
                </div>
                <div class="stat-item">
                    <span class="stat-label">월 비용</span>
                    <span class="stat-value highlight">${rec.monthlyCost.toLocaleString()}<small>원</small></span>
                </div>
            </div>

            <div class="nutrition-bars">
                <div class="nutrition-bar-item">
                    <div class="bar-label"><span>단백질</span><span>${rec.proteinPercent}%</span></div>
                    <div class="bar-track"><div class="bar-fill protein" style="width: ${proteinBarWidth}%"></div></div>
                </div>
                <div class="nutrition-bar-item">
                    <div class="bar-label"><span>지방</span><span>${rec.fatPercent}%</span></div>
                    <div class="bar-track"><div class="bar-fill fat" style="width: ${fatBarWidth}%"></div></div>
                </div>
            </div>

            <div class="card-footer">
                <span class="reason-text"><span class="reason-label">추천 이유</span>${rec.reason}</span>
            </div>
        `;

        list.appendChild(card);
    });
}

// ===== 이벤트 바인딩 =====
document.getElementById('recommendBtn').addEventListener('click', recommend);

document.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        const active = document.activeElement;
        if (active && (active.tagName === 'INPUT' || active.tagName === 'SELECT')) {
            e.preventDefault();
            recommend();
        }
    }
});
