// Animation Observer Class untuk mengelola animasi scroll
class AnimationObserver {
    constructor() {
        this.observer = null;
        this.init();
    }

    init() {
        // Konfigurasi Intersection Observer
        const options = {
            root: null,
            rootMargin: '0px 0px -50px 0px',
            threshold: 0.1
        };

        this.observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('animate');
                }
            });
        }, options);

        // Observe semua element dengan animation classes
        this.observeElements();
    }

    observeElements() {
        const animatedElements = document.querySelectorAll(
            '.fade-in-up, .fade-in-left, .fade-in-right, .scale-in, .slide-in-bottom'
        );

        animatedElements.forEach(element => {
            this.observer.observe(element);
        });
    }

    // Method untuk observe element baru (untuk konten dinamis)
    observeNewElements(container) {
        const newElements = container.querySelectorAll(
            '.fade-in-up, .fade-in-left, .fade-in-right, .scale-in, .slide-in-bottom'
        );
        
        newElements.forEach(element => {
            this.observer.observe(element);
        });
    }

    // Trigger animasi langsung tanpa scroll
    triggerAnimation(selector) {
        const elements = document.querySelectorAll(selector);
        elements.forEach(element => {
            element.classList.add('animate');
        });
    }
}

// Page Loader Management
class PageLoader {
    constructor() {
        this.loader = document.getElementById('pageLoader');
        this.init();
    }

    init() {
        // Hide loader ketika page sudah fully loaded
        window.addEventListener('load', () => {
            setTimeout(() => {
                this.hideLoader();
            }, 800); // Delay untuk effect yang smooth
        });
    }

    hideLoader() {
        if (this.loader) {
            this.loader.classList.add('hidden');
            
            // Remove dari DOM setelah transition selesai
            setTimeout(() => {
                this.loader.style.display = 'none';
            }, 500);
        }
    }

    showLoader() {
        if (this.loader) {
            this.loader.style.display = 'flex';
            this.loader.classList.remove('hidden');
        }
    }
}

// Hover Effects Management
class HoverEffectsManager {
    constructor() {
        this.init();
    }

    init() {
        this.initTableRowEffects();
        this.initButtonEffects();
        this.initStatItemEffects();
        this.initModalEffects();
    }

    initTableRowEffects() {
        // Add hover sound effect untuk table rows
        const tableRows = document.querySelectorAll('.orders-table tbody tr');
        
        tableRows.forEach(row => {
            row.addEventListener('mouseenter', () => {
                row.style.transform = 'scale(1.01)';
                row.style.transition = 'all 0.3s ease';
            });
            
            row.addEventListener('mouseleave', () => {
                row.style.transform = 'scale(1)';
            });
        });
    }

    initButtonEffects() {
        // Enhanced button hover effects
        const buttons = document.querySelectorAll('.btn, .export-btn, .refresh-btn, .action-btn');
        
        buttons.forEach(button => {
            button.addEventListener('mouseenter', (e) => {
                e.target.style.transform = 'translateY(-2px)';
                e.target.style.boxShadow = '0 8px 25px rgba(0, 0, 0, 0.15)';
            });
            
            button.addEventListener('mouseleave', (e) => {
                e.target.style.transform = 'translateY(0)';
                e.target.style.boxShadow = '';
            });
        });
    }

    initStatItemEffects() {
        // Stat items dengan floating animation
        const statItems = document.querySelectorAll('.stat-item');
        
        statItems.forEach((item, index) => {
            // Floating animation dengan delay berbeda
            item.style.animationDelay = `${index * 0.2}s`;
            item.classList.add('floating-item');
            
            item.addEventListener('click', () => {
                // Pulse effect saat diklik
                item.style.transform = 'scale(1.1)';
                setTimeout(() => {
                    item.style.transform = 'scale(1)';
                }, 200);
            });
        });
    }

    initModalEffects() {
        // Modal appearance effects
        const modals = document.querySelectorAll('.modal-overlay');
        
        modals.forEach(modal => {
            const content = modal.querySelector('.modal-content');
            
            modal.addEventListener('click', (e) => {
                if (e.target === modal) {
                    this.closeModalWithAnimation(modal);
                }
            });
        });
    }

    closeModalWithAnimation(modal) {
        const content = modal.querySelector('.modal-content');
        content.style.transform = 'scale(0.8)';
        content.style.opacity = '0';
        
        setTimeout(() => {
            modal.classList.remove('active');
            content.style.transform = 'scale(1)';
            content.style.opacity = '1';
        }, 300);
    }
}

// Sidebar Animation Management
class SidebarAnimations {
    constructor() {
        this.sidebar = document.getElementById('sidebar');
        this.sidebarToggle = document.getElementById('sidebarToggle');
        this.init();
    }

    init() {
        if (this.sidebarToggle) {
            this.sidebarToggle.addEventListener('click', () => {
                this.toggleSidebar();
            });
        }

        // Nav link animations
        this.initNavLinkEffects();
    }

    toggleSidebar() {
        if (this.sidebar) {
            this.sidebar.classList.toggle('collapsed');
            
            // Animate nav items
            const navItems = this.sidebar.querySelectorAll('.nav-item');
            navItems.forEach((item, index) => {
                setTimeout(() => {
                    item.style.transform = this.sidebar.classList.contains('collapsed') 
                        ? 'translateX(-10px)' 
                        : 'translateX(0)';
                }, index * 50);
            });
        }
    }

    initNavLinkEffects() {
        const navLinks = document.querySelectorAll('.nav-link');
        
        navLinks.forEach(link => {
            link.addEventListener('mouseenter', () => {
                link.style.transform = 'translateX(10px)';
                link.style.transition = 'all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94)';
            });
            
            link.addEventListener('mouseleave', () => {
                if (!link.classList.contains('active')) {
                    link.style.transform = 'translateX(0)';
                }
            });
        });
    }
}

// Search Animation Effects
class SearchAnimations {
    constructor() {
        this.searchInput = document.getElementById('searchInput');
        this.searchBtn = document.getElementById('searchBtn');
        this.init();
    }

    init() {
        if (this.searchInput) {
            this.searchInput.addEventListener('focus', () => {
                this.searchInput.parentElement.style.transform = 'scale(1.02)';
                this.searchInput.parentElement.style.transition = 'all 0.3s ease';
            });
            
            this.searchInput.addEventListener('blur', () => {
                this.searchInput.parentElement.style.transform = 'scale(1)';
            });
        }

        if (this.searchBtn) {
            this.searchBtn.addEventListener('click', () => {
                // Pulse animation untuk search button
                this.searchBtn.style.transform = 'scale(0.95)';
                setTimeout(() => {
                    this.searchBtn.style.transform = 'scale(1)';
                }, 150);
            });
        }
    }
}

// Main Initialization
document.addEventListener('DOMContentLoaded', () => {
    // Initialize semua animation systems
    const animationObserver = new AnimationObserver();
    const pageLoader = new PageLoader();
    const hoverEffects = new HoverEffectsManager();
    const sidebarAnimations = new SidebarAnimations();
    const searchAnimations = new SearchAnimations();

    // Initial trigger untuk elements yang sudah terlihat
    setTimeout(() => {
        animationObserver.triggerAnimation('.fade-in-left, .fade-in-right, .fade-in-up');
    }, 100);

    // Trigger stagger animations untuk stat items
    setTimeout(() => {
        animationObserver.triggerAnimation('.stagger-animation');
    }, 500);

    // Trigger table animation
    setTimeout(() => {
        animationObserver.triggerAnimation('.slide-in-bottom');
    }, 800);

    // Global animation observer untuk dynamic content
    window.adminAnimations = {
        observer: animationObserver,
        pageLoader: pageLoader,
        hoverEffects: hoverEffects,
        observeNewElements: (container) => animationObserver.observeNewElements(container)
    };

    // Smooth scroll untuk semua internal links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });

    console.log('Admin Animations System initialized successfully!');
});

// CSS untuk floating animation (inject ke head)
const floatingCSS = `
    .floating-item {
        animation: float 3s ease-in-out infinite;
    }
    
    @keyframes float {
        0%, 100% { transform: translateY(0px); }
        50% { transform: translateY(-5px); }
    }
`;

// Inject CSS
const style = document.createElement('style');
style.textContent = floatingCSS;
document.head.appendChild(style);
