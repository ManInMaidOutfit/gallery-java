const API_BASE = "";
let token = null;
let currentPage = 0;
const PAGE_SIZE = 12;
let currentPhotos = [];

// ========== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ==========

function escapeHtml(str) {
    if (!str) return '';
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function logout() {
    token = null;
    localStorage.removeItem("gallery_token");
    
    const adminPanel = document.getElementById("adminPanel");
    const uploadSection = document.getElementById("uploadSection");
    const loginForm = document.getElementById("loginForm");
    const categoryEditor = document.getElementById("categoryEditorSection");
    
    if (adminPanel) adminPanel.style.display = "none";
    if (uploadSection) uploadSection.style.display = "none";
    if (loginForm) loginForm.style.display = "flex";
    if (categoryEditor) categoryEditor.remove();
    
    document.getElementById("username").value = "";
    document.getElementById("password").value = "";
    
    currentPage = 0;
    loadPhotos();
}

// ========== ЗАГРУЗКА КАТЕГОРИЙ ==========

async function loadCategories() {
    try {
        const response = await fetch(`${API_BASE}/categories`);
        const data = await response.json();
        
        const categorySelect = document.getElementById("categorySelect");
        const categoryFilter = document.getElementById("categoryFilter");
        
        if (categorySelect) {
            categorySelect.innerHTML = '<option value="">Выберите категорию</option>';
        }
        if (categoryFilter) {
            categoryFilter.innerHTML = '<option value="">Все категории</option>';
        }
        
        data.forEach(cat => {
            const option = new Option(escapeHtml(cat.title), cat.id);
            if (categorySelect) categorySelect.appendChild(option.cloneNode(true));
            if (categoryFilter) categoryFilter.appendChild(option.cloneNode(true));
        });
    } catch (error) {
        console.error("Ошибка загрузки категорий:", error);
    }
}

// Получение названия категории по ID
async function getCategoryTitle(categoryId) {
    if (!categoryId) return 'По умолчанию';
    try {
        const response = await fetch(`${API_BASE}/categories/${categoryId}`);
        if (response.ok) {
            const category = await response.json();
            return category.title;
        }
        return 'По умолчанию';
    } catch (error) {
        return 'По умолчанию';
    }
}

// ========== ЗАГРУЗКА ФОТО (СЕРВЕРНАЯ ПАГИНАЦИЯ) ==========

async function loadPhotos() {
    const search = document.getElementById("searchInput")?.value || "";
    const category = document.getElementById("categoryFilter")?.value || "";
    
    let url;
    if (category) {
        url = `${API_BASE}/photos/by-category/${category}?page=${currentPage}&size=${PAGE_SIZE}`;
    } else {
        url = `${API_BASE}/photos?page=${currentPage}&size=${PAGE_SIZE}`;
    }
    if (search) url += `&search=${encodeURIComponent(search)}`;
    
    try {
        const response = await fetch(url);
        const data = await response.json();
        
        const photos = data.content || [];
        const totalPages = data.totalPages || 0;
        
        // Загружаем названия категорий для каждого фото
        for (const photo of photos) {
            if (photo.categoryId && !photo.categoryTitle) {
                photo.categoryTitle = await getCategoryTitle(photo.categoryId);
            }
        }
        
        let filteredPhotos = photos;
        if (!token) {
            filteredPhotos = photos.filter(photo => photo.isVisible === true);
        }
        
        currentPhotos = filteredPhotos;
        renderGallery(currentPhotos);
        renderPagination(totalPages, data.totalElements);
        
    } catch (error) {
        console.error("Ошибка загрузки фото:", error);
        const gallery = document.getElementById("gallery");
        if (gallery) {
            gallery.innerHTML = '<div class="loading">Ошибка загрузки фото. Проверьте соединение с сервером.</div>';
        }
    }
}

// ========== ПАГИНАЦИЯ ==========

function renderPagination(totalPages, totalElements) {
    const pagination = document.getElementById("pagination");
    if (!pagination) return;
    pagination.innerHTML = "";
    
    if (totalPages <= 1 && totalElements === 0) return;
    
    if (currentPage > 0) {
        const prevBtn = document.createElement("button");
        prevBtn.textContent = "◀ Назад";
        prevBtn.onclick = () => {
            currentPage--;
            loadPhotos();
            window.scrollTo({ top: 0, behavior: 'smooth' });
        };
        pagination.appendChild(prevBtn);
    }
    
    const pageInfo = document.createElement("span");
    pageInfo.textContent = `Страница ${currentPage + 1} из ${totalPages}`;
    pagination.appendChild(pageInfo);
    
    if (currentPage < totalPages - 1) {
        const nextBtn = document.createElement("button");
        nextBtn.textContent = "Вперед ▶";
        nextBtn.onclick = () => {
            currentPage++;
            loadPhotos();
            window.scrollTo({ top: 0, behavior: 'smooth' });
        };
        pagination.appendChild(nextBtn);
    }
}

// ========== ОТОБРАЖЕНИЕ ГАЛЕРЕИ ==========

function renderGallery(photos) {
    const gallery = document.getElementById("gallery");
    if (!gallery) return;
    gallery.innerHTML = "";
    
    if (photos.length === 0) {
        gallery.innerHTML = '<div class="loading">📷 Нет фото. Загрузите первое!</div>';
        return;
    }
    
    photos.forEach((photo, idx) => {
        const card = document.createElement("div");
        card.className = "photo-card";
        
        if (token && photo.isVisible === false) {
            card.classList.add("invisible-photo");
        }
        
        const categoryTitle = photo.categoryTitle || 'По умолчанию';
        
        card.innerHTML = `
            <img src="${API_BASE}/photos/image/${photo.id}" alt="${escapeHtml(photo.title)}" data-id="${photo.id}" data-index="${idx}" loading="lazy">
            <div class="photo-info">
                <div class="photo-title-wrapper">
                    <div class="photo-title">
                        ${escapeHtml(photo.title)}
                        ${token && photo.isVisible === false ? ' <span class="hidden-badge">(СКРЫТО)</span>' : ''}
                    </div>
                    <button class="desc-btn" data-title="${escapeHtml(photo.title)}" data-desc="${escapeHtml(photo.description || '')}">
                        <i>ⓘ</i>
                    </button>
                </div>
                <div class="photo-category">${escapeHtml(categoryTitle)}</div>
                <div class="photo-views">👁️ ${photo.views || 0} просмотров</div>
                ${token ? `
                    <div class="photo-actions">
                        <button class="edit-btn" data-id="${photo.id}">Редактировать</button>
                        <button class="delete-btn" data-id="${photo.id}">Удалить</button>
                    </div>
                ` : ""}
            </div>
        `;
        gallery.appendChild(card);
        
        const img = card.querySelector("img");
        if (img) {
            img.addEventListener("click", (e) => {
                e.stopPropagation();
                if (!token && photo.isVisible === false) {
                    alert("Это фото недоступно для просмотра");
                    return;
                }
                openSlider(parseInt(img.dataset.index));
            });
        }
        
        const descBtn = card.querySelector(".desc-btn");
        if (descBtn) {
            descBtn.addEventListener("click", (e) => {
                e.stopPropagation();
                openDescriptionModal(descBtn.dataset.title, descBtn.dataset.desc);
            });
        }
        
        const editBtn = card.querySelector(".edit-btn");
        if (editBtn) {
            editBtn.addEventListener("click", (e) => {
                e.stopPropagation();
                openEditModal(parseInt(editBtn.dataset.id));
            });
        }
        
        const deleteBtn = card.querySelector(".delete-btn");
        if (deleteBtn) {
            deleteBtn.addEventListener("click", (e) => {
                e.stopPropagation();
                deletePhoto(parseInt(deleteBtn.dataset.id));
            });
        }
    });
}

// ========== СЛАЙДЕР ==========

let currentSlideIndex = 0;

function openSlider(index) {
    const photo = currentPhotos[index];
    
    if (!token && photo.isVisible === false) {
        alert("Это фото недоступно для просмотра");
        return;
    }
    
    currentSlideIndex = index;
    const modal = document.getElementById("sliderModal");
    const img = document.getElementById("sliderImage");
    const caption = document.getElementById("sliderCaption");
    
    if (!modal || !img || !caption) return;
    
    const currentPhoto = currentPhotos[currentSlideIndex];
    img.src = `${API_BASE}/photos/image/${currentPhoto.id}`;
    img.alt = escapeHtml(currentPhoto.title);
    caption.textContent = currentPhoto.title;
    modal.style.display = "block";
    document.body.style.overflow = "hidden";
    
    fetch(`${API_BASE}/photos/${currentPhoto.id}/view`, { method: "POST" })
        .catch(e => console.error("Ошибка увеличения просмотров:", e));
    
    currentPhoto.views = (currentPhoto.views || 0) + 1;
    const viewElement = document.querySelector(`.photo-card img[data-id="${currentPhoto.id}"]`)
        ?.closest(".photo-card")?.querySelector(".photo-views");
    if (viewElement) {
        viewElement.textContent = `👁️ ${currentPhoto.views} просмотров`;
    }
}

function closeSlider() {
    const modal = document.getElementById("sliderModal");
    if (modal) {
        modal.style.display = "none";
        document.body.style.overflow = "auto";
    }
}

function changeSlide(direction) {
    if (!currentPhotos.length) return;
    
    currentSlideIndex += direction;
    if (currentSlideIndex < 0) currentSlideIndex = currentPhotos.length - 1;
    if (currentSlideIndex >= currentPhotos.length) currentSlideIndex = 0;
    
    const img = document.getElementById("sliderImage");
    const caption = document.getElementById("sliderCaption");
    if (!img || !caption) return;
    
    const photo = currentPhotos[currentSlideIndex];
    if (!token && photo.isVisible === false) {
        closeSlider();
        alert("Это фото недоступно для просмотра");
        return;
    }
    img.src = `${API_BASE}/photos/image/${photo.id}`;
    img.alt = escapeHtml(photo.title);
    caption.textContent = photo.title;
}

// ========== МОДАЛЬНОЕ ОКНО ОПИСАНИЯ ==========

function openDescriptionModal(title, description) {
    const modal = document.getElementById("descModal");
    const modalTitle = document.getElementById("descModalTitle");
    const modalText = document.getElementById("descModalText");
    
    if (!modal || !modalTitle || !modalText) return;
    
    modalTitle.textContent = title;
    modalText.textContent = description || "Описание отсутствует";
    modal.style.display = "block";
    document.body.style.overflow = "hidden";
}

function closeDescriptionModal() {
    const modal = document.getElementById("descModal");
    if (modal) {
        modal.style.display = "none";
        document.body.style.overflow = "auto";
    }
}

// ========== АВТОРИЗАЦИЯ ==========

document.getElementById("loginBtn")?.addEventListener("click", async () => {
    const username = document.getElementById("username")?.value || "";
    const password = document.getElementById("password")?.value || "";
    
    if (!username || !password) {
        alert("Введите логин и пароль");
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/auth/login?username=${username}&password=${password}`, {
            method: "POST"
        });
        
        if (response.ok) {
            const data = await response.json();
            token = data.access_token;
            localStorage.setItem("gallery_token", token);
            
            document.getElementById("adminPanel").style.display = "flex";
            document.getElementById("uploadSection").style.display = "block";
            document.getElementById("loginForm").style.display = "none";
            document.getElementById("username").value = "";
            document.getElementById("password").value = "";
            
            alert("Вход выполнен успешно!");
            
            createCategoryEditorSection();
            currentPage = 0;
            loadPhotos();
        } else {
            const errorText = await response.text();
            alert(`Ошибка входа: ${errorText || "неверные учётные данные"}`);
        }
    } catch (error) {
        console.error("Ошибка входа:", error);
        alert("Ошибка соединения с сервером");
    }
});

document.getElementById("logoutBtn")?.addEventListener("click", () => {
    logout();
    alert("Вы вышли из аккаунта");
});

// ========== ЗАГРУЗКА ФОТО ==========

document.getElementById("uploadForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    
    if (!token) {
        alert("Сначала войдите как администратор");
        return;
    }
    
    const title = document.getElementById("photoTitle")?.value.trim();
    const file = document.getElementById("photoFile")?.files[0];
    
    if (!title || !file) {
        alert("Заполните название и выберите файл");
        return;
    }
    
    if (!file.type.startsWith('image/')) {
        alert("Пожалуйста, выберите изображение");
        return;
    }
    
    const formData = new FormData();
    formData.append("file", file);
    formData.append("title", title);
    formData.append("description", document.getElementById("photoDesc")?.value || "");
    const categoryId = document.getElementById("categorySelect")?.value;
    if (categoryId) formData.append("categoryId", categoryId);
    
    const submitBtn = e.target.querySelector('button[type="submit"]');
    const originalText = submitBtn.textContent;
    submitBtn.textContent = "Загрузка...";
    submitBtn.disabled = true;
    
    try {
        const response = await fetch(`${API_BASE}/photos/upload`, {
            method: "POST",
            headers: { "Authorization": `Bearer ${token}` },
            body: formData
        });
        
        if (response.ok) {
            alert("Фото успешно загружено!");
            e.target.reset();
            document.getElementById('fileName').textContent = 'Файл не выбран';
            currentPage = 0;
            loadPhotos();
        } else {
            const errorText = await response.text();
            alert(`Ошибка загрузки: ${errorText}`);
        }
    } catch (error) {
        console.error("Ошибка загрузки:", error);
        alert("Ошибка соединения с сервером");
    } finally {
        submitBtn.textContent = originalText;
        submitBtn.disabled = false;
    }
});

async function deletePhoto(photoId) {
    if (!confirm("Вы уверены, что хотите удалить это фото?")) return;
    if (!token) {
        alert("Ошибка авторизации");
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/photos/${photoId}`, {
            method: "DELETE",
            headers: { "Authorization": `Bearer ${token}` }
        });
        
        if (response.ok) {
            alert("Фото удалено");
            loadPhotos();
        } else {
            const errorText = await response.text();
            alert(`Ошибка удаления: ${errorText}`);
        }
    } catch (error) {
        console.error("Ошибка удаления:", error);
        alert("Ошибка соединения с сервером");
    }
}

// ========== РЕДАКТОР КАТЕГОРИЙ ==========

function createCategoryEditorSection() {
    const uploadSection = document.getElementById("uploadSection");
    if (!uploadSection) return;
    if (document.getElementById("categoryEditorSection")) return;
    
    const editorSection = document.createElement("div");
    editorSection.id = "categoryEditorSection";
    editorSection.className = "upload-section";
    editorSection.style.marginTop = "10px";
    editorSection.innerHTML = `
        <h3>Редактор категорий</h3>
        <div class="category-editor">
            <div class="category-add-form">
                <input type="text" id="newCategoryTitle" placeholder="Название новой категории" maxlength="50">
                <button id="addCategoryBtn" class="category-add-btn">Добавить категорию</button>
            </div>
            <div class="category-list" id="categoryList">
                <div class="loading-categories">Загрузка категорий...</div>
            </div>
        </div>
    `;
    
    uploadSection.insertAdjacentElement('afterend', editorSection);
    
    document.getElementById("addCategoryBtn")?.addEventListener("click", addCategory);
    loadCategoriesForAdmin();
}

async function loadCategoriesForAdmin() {
    try {
        const response = await fetch(`${API_BASE}/categories`);
        const categories = await response.json();
        window.categoriesList = categories;
        renderCategoryList(categories);
    } catch (error) {
        console.error("Ошибка загрузки категорий:", error);
        const categoryList = document.getElementById("categoryList");
        if (categoryList) {
            categoryList.innerHTML = '<div class="error-message">Ошибка загрузки категорий</div>';
        }
    }
}

function renderCategoryList(categories) {
    const container = document.getElementById("categoryList");
    if (!container) return;
    
    if (!categories || categories.length === 0) {
        container.innerHTML = '<div class="empty-categories">📭 Нет категорий. Создайте первую!</div>';
        return;
    }
    
    container.innerHTML = `
        <div class="category-header">
            <span>Название категории</span>
            <span>Действия</span>
        </div>
        ${categories.map(cat => `
            <div class="category-item" data-id="${cat.id}">
                <div class="category-title">
                    ${window.editingCategoryId === cat.id ? 
                        `<input type="text" class="category-edit-input" id="editInput_${cat.id}" value="${escapeHtml(cat.title)}" maxlength="50">` :
                        `<span class="category-name">${escapeHtml(cat.title)}</span>
                         ${cat.id === 1 ? '<span class="default-badge">По умолчанию</span>' : ''}`
                    }
                </div>
                <div class="category-actions">
                    ${window.editingCategoryId === cat.id ? 
                        `<button class="category-save-btn" data-id="${cat.id}">Сохранить</button>
                         <button class="category-cancel-btn" data-id="${cat.id}">❌ Отмена</button>` :
                        `<button class="category-edit-btn" data-id="${cat.id}" ${cat.id === 1 ? 'disabled title="Категорию по умолчанию нельзя редактировать"' : ''}>Редактировать</button>
                         <button class="category-delete-btn" data-id="${cat.id}" ${cat.id === 1 ? 'disabled title="Категорию по умолчанию нельзя удалить"' : ''}>Удалить</button>`
                    }
                </div>
            </div>
        `).join('')}
    `;
    
    document.querySelectorAll('.category-edit-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            window.editingCategoryId = parseInt(btn.dataset.id);
            renderCategoryList(categories);
        });
    });
    
    document.querySelectorAll('.category-delete-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            deleteCategory(parseInt(btn.dataset.id));
        });
    });
    
    document.querySelectorAll('.category-save-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const id = parseInt(btn.dataset.id);
            const input = document.getElementById(`editInput_${id}`);
            if (input) updateCategory(id, input.value.trim());
        });
    });
    
    document.querySelectorAll('.category-cancel-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            window.editingCategoryId = null;
            renderCategoryList(categories);
        });
    });
}

async function addCategory() {
    const titleInput = document.getElementById("newCategoryTitle");
    const title = titleInput?.value.trim();
    
    if (!title) {
        alert("Введите название категории");
        return;
    }
    
    if (!token) {
        alert("Ошибка: вы не авторизованы");
        return;
    }
    
    const addBtn = document.getElementById("addCategoryBtn");
    addBtn.disabled = true;
    addBtn.textContent = "Добавление...";
    
    try {
        const response = await fetch(`${API_BASE}/categories?title=${encodeURIComponent(title)}`, {
            method: "POST",
            headers: { "Authorization": `Bearer ${token}` }
        });
        
        const result = await response.text();
        
        if (response.ok) {
            alert(result || `Категория "${title}" успешно добавлена!`);
            titleInput.value = "";
            await loadCategories();
            await loadCategoriesForAdmin();
            currentPage = 0;
            await loadPhotos();
        } else {
            alert(`Ошибка добавления категории (${response.status}): ${result}`);
        }
    } catch (error) {
        console.error("Ошибка добавления категории:", error);
        alert("Ошибка соединения с сервером");
    } finally {
        addBtn.disabled = false;
        addBtn.textContent = "Добавить категорию";
    }
}

async function updateCategory(id, newTitle) {
    if (!newTitle) {
        alert("Название категории не может быть пустым");
        window.editingCategoryId = null;
        loadCategoriesForAdmin();
        return;
    }
    
    if (!token) {
        alert("Ошибка авторизации");
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/categories/${id}?title=${encodeURIComponent(newTitle)}`, {
            method: "PUT",
            headers: { "Authorization": `Bearer ${token}` }
        });
        
        const result = await response.text();
        alert(result);
        
        if (response.ok) {
            window.editingCategoryId = null;
            await loadCategories();
            await loadCategoriesForAdmin();
            currentPage = 0;
            await loadPhotos();
        }
    } catch (error) {
        console.error("Ошибка обновления категории:", error);
        alert("Ошибка соединения с сервером");
        window.editingCategoryId = null;
        loadCategoriesForAdmin();
    }
}

async function deleteCategory(id) {
    const category = window.categoriesList?.find(c => c.id === id);
    if (!category) return;
    
    if (!confirm(`Вы уверены, что хотите удалить категорию "${category.title}"?\n\nВсе фото из этой категории будут перемещены в категорию "По умолчанию".`)) return;
    if (!token) {
        alert("Ошибка авторизации");
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/categories/${id}`, {
            method: "DELETE",
            headers: { "Authorization": `Bearer ${token}` }
        });
        
        const result = await response.text();
        alert(result);
        
        if (response.ok) {
            await loadCategories();
            await loadCategoriesForAdmin();
            currentPage = 0;
            await loadPhotos();
        }
    } catch (error) {
        console.error("Ошибка удаления категории:", error);
        alert("Ошибка соединения с сервером");
    }
}

// ========== РЕДАКТИРОВАНИЕ ФОТО ==========

async function openEditModal(photoId) {
    const photo = currentPhotos.find(p => p.id === photoId);
    if (!photo) {
        alert("Ошибка: фото не найдено");
        return;
    }
    
    if (!token) {
        alert("Ошибка: вы не авторизованы");
        return;
    }
    
    const existingModal = document.getElementById("editPhotoModal");
    if (existingModal) existingModal.remove();
    
    const editModal = document.createElement("div");
    editModal.id = "editPhotoModal";
    editModal.className = "modal";
    editModal.innerHTML = `
        <div class="modal-content">
            <span class="close-modal">&times;</span>
            <h3>Редактировать фото</h3>
            <form id="editPhotoForm">
                <input type="text" id="editTitle" placeholder="Название" required value="${escapeHtml(photo.title)}">
                <input type="text" id="editDescription" placeholder="Описание" value="${escapeHtml(photo.description || '')}">
                <select id="editCategoryId"></select>
                <div class="checkbox-group">
                    <label>
                        <input type="checkbox" id="editIsVisible" ${photo.isVisible !== false ? 'checked' : ''}> Видимо для всех
                    </label>
                </div>
                <div class="file-wrapper">
                    <label for="editPhotoFile" class="file-label">Заменить изображение</label>
                    <input type="file" id="editPhotoFile" accept="image/*" style="display: none;">
                    <span id="editFileName" class="file-name">Файл не выбран</span>
                </div>
                <button type="submit">Сохранить изменения</button>
                <button type="button" id="cancelEditBtn" class="cancel-btn">Отмена</button>
            </form>
        </div>
    `;
    document.body.appendChild(editModal);
    
    try {
        const response = await fetch(`${API_BASE}/categories`);
        const categories = await response.json();
        const select = document.getElementById("editCategoryId");
        if (select) {
            select.innerHTML = '<option value="">Без категории</option>';
            categories.forEach(cat => {
                const option = new Option(escapeHtml(cat.title), cat.id);
                select.appendChild(option);
            });
            if (photo.categoryId) select.value = photo.categoryId;
        }
    } catch (error) {
        console.error("Ошибка загрузки категорий:", error);
    }
    
    editModal.querySelector(".close-modal").onclick = () => closeEditModal();
    document.getElementById("cancelEditBtn").onclick = () => closeEditModal();
    editModal.onclick = (e) => { if (e.target === editModal) closeEditModal(); };
    
    document.getElementById("editPhotoFile").addEventListener('change', function() {
        const fileNameSpan = document.getElementById("editFileName");
        if (this.files && this.files[0]) {
            fileNameSpan.textContent = this.files[0].name;
        } else {
            fileNameSpan.textContent = 'Файл не выбран';
        }
    });
    
    document.getElementById("editPhotoForm").onsubmit = async (e) => {
        e.preventDefault();
        
        const title = document.getElementById("editTitle")?.value.trim();
        if (!title) {
            alert("Введите название фото");
            return;
        }
        
        const formData = new FormData();
        formData.append("title", title);
        const description = document.getElementById("editDescription")?.value.trim();
        if (description) formData.append("description", description);
        const categoryId = document.getElementById("editCategoryId")?.value;
        if (categoryId) formData.append("categoryId", categoryId);
        formData.append("isVisible", document.getElementById("editIsVisible")?.checked === true);
        const file = document.getElementById("editPhotoFile")?.files[0];
        if (file) formData.append("file", file);
        
        const submitBtn = e.target.querySelector('button[type="submit"]');
        const originalText = submitBtn.textContent;
        submitBtn.textContent = "Сохранение...";
        submitBtn.disabled = true;
        
        try {
            const response = await fetch(`${API_BASE}/photos/${photoId}`, {
                method: "PATCH",
                headers: { "Authorization": `Bearer ${token}` },
                body: formData
            });
            
            const result = await response.text();
            
            if (response.ok) {
                alert("Фото успешно обновлено!");
                closeEditModal();
                currentPage = 0;
                loadPhotos();
            } else {
                alert(`Ошибка обновления (${response.status}): ${result}`);
            }
        } catch (error) {
            console.error("Ошибка обновления:", error);
            alert("Ошибка соединения с сервером");
        } finally {
            submitBtn.textContent = originalText;
            submitBtn.disabled = false;
        }
    };
    
    editModal.style.display = "block";
    document.body.style.overflow = "hidden";
}

function closeEditModal() {
    const modal = document.getElementById("editPhotoModal");
    if (modal) {
        modal.style.display = "none";
        document.body.style.overflow = "auto";
        modal.remove();
    }
}

// ========== ПОИСК И ФИЛЬТРАЦИЯ ==========

let searchTimeout;
document.getElementById("searchInput")?.addEventListener("input", () => {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
        currentPage = 0;
        loadPhotos();
    }, 500);
});

document.getElementById("categoryFilter")?.addEventListener("change", () => {
    currentPage = 0;
    loadPhotos();
});

// ========== СЛАЙДЕР: ЗАКРЫТИЕ И НАВИГАЦИЯ ==========

document.querySelector(".close-slider")?.addEventListener("click", closeSlider);
document.getElementById("sliderModal")?.addEventListener("click", (e) => {
    if (e.target === e.currentTarget) closeSlider();
});
document.querySelector(".slider-prev")?.addEventListener("click", () => changeSlide(-1));
document.querySelector(".slider-next")?.addEventListener("click", () => changeSlide(1));
document.addEventListener("keydown", (e) => {
    const modal = document.getElementById("sliderModal");
    if (modal && modal.style.display === "block") {
        if (e.key === "ArrowLeft") { e.preventDefault(); changeSlide(-1); }
        if (e.key === "ArrowRight") { e.preventDefault(); changeSlide(1); }
        if (e.key === "Escape") closeSlider();
    }
});

// ========== ОБРАБОТЧИКИ МОДАЛЬНОГО ОКНА ОПИСАНИЯ ==========

document.querySelector(".close-desc-modal")?.addEventListener("click", closeDescriptionModal);
document.getElementById("descModal")?.addEventListener("click", (e) => {
    if (e.target === e.currentTarget) closeDescriptionModal();
});

// ========== ИНИЦИАЛИЗАЦИЯ ==========

document.getElementById('photoFile')?.addEventListener('change', function() {
    const fileNameSpan = document.getElementById('fileName');
    if (this.files && this.files[0]) {
        fileNameSpan.textContent = this.files[0].name;
    } else {
        fileNameSpan.textContent = 'Файл не выбран';
    }
});

function loadSavedToken() {
    const savedToken = localStorage.getItem("gallery_token");
    if (savedToken) {
        token = savedToken;
        document.getElementById("adminPanel").style.display = "flex";
        document.getElementById("uploadSection").style.display = "block";
        document.getElementById("loginForm").style.display = "none";
        createCategoryEditorSection();
    }
    loadPhotos();
}

loadCategories();
loadSavedToken();