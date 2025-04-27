import tkinter as tk
from tkinter import ttk
from datetime import datetime
import getpass
import os
from PIL import Image, ImageTk
import glob

class WelcomeApp:
    def __init__(self, root):
        self.root = root
        self.root.title("Welcome")

        # Установка пользовательской иконки
        try:
            self.root.iconbitmap("icon.ico")
        except tk.TclError:
            print("Не удалось загрузить иконку. Убедитесь, что файл 'icon.ico' находится в папке со скриптом.")

        # Настройки окна
        self.root.geometry("500x300")
        self.root.configure(bg="#f0f0f0")

        # Градиентный фон (реализуем через Canvas)
        self.canvas = tk.Canvas(self.root, width=500, height=300, highlightthickness=0)
        self.canvas.pack(fill="both", expand=True)
        self.create_gradient(self.canvas, "#ffcc80", "#ff80ab")  # Оранжевый до розового

        # Контейнер для текста (без альфа-канала, используем светлый фон)
        self.frame = tk.Frame(self.canvas, bg="#f5f5f5", bd=0)
        self.frame.place(relx=0.5, rely=0.5, anchor="center", width=400, height=200)

        # Получаем имя пользователя
        user_name = getpass.getuser()

        # Получаем текущее время для персонализации
        current_hour = datetime.now().hour
        if current_hour < 12:
            greeting = "Доброе утро"
        elif current_hour < 18:
            greeting = "Добрый день"
        else:
            greeting = "Добрый вечер"

        # Текст приветствия
        self.label = tk.Label(
            self.frame,
            text=f"{greeting}, {user_name}!\nДобро пожаловать!",
            font=("Helvetica", 16, "bold"),
            bg="#f5f5f5",
            fg="#333333",
            pady=20,
            justify="center"
        )
        self.label.pack(expand=True)

        # Кнопка "Старт" с анимацией
        self.start_button = tk.Button(
            self.frame,
            text="Старт",
            command=self.open_image_viewer,
            font=("Helvetica", 12),
            bg="#4CAF50",
            fg="white",
            bd=0,
            padx=20,
            pady=10,
            relief="flat"
        )
        self.start_button.pack(pady=10)
        self.start_button.bind("<Enter>", lambda e: self.start_button.configure(bg="#45a049"))
        self.start_button.bind("<Leave>", lambda e: self.start_button.configure(bg="#4CAF50"))

        # Центрируем окно на экране
        self.center_window(self.root)

    def create_gradient(self, canvas, color1, color2):
        """Создает градиентный фон на Canvas"""
        canvas.delete("all")
        for i in range(300):
            r1, g1, b1 = tuple(int(color1.lstrip("#")[j:j+2], 16) for j in (0, 2, 4))
            r2, g2, b2 = tuple(int(color2.lstrip("#")[j:j+2], 16) for j in (0, 2, 4))
            r = int(r1 + (r2 - r1) * i / 300)
            g = int(g1 + (g2 - g1) * i / 300)
            b = int(b1 + (b2 - b1) * i / 300)
            color = f"#{r:02x}{g:02x}{b:02x}"
            canvas.create_line(0, i, 500, i, fill=color)

    def center_window(self, window):
        window.update_idletasks()
        width = window.winfo_width()
        height = window.winfo_height()
        x = (window.winfo_screenwidth() // 2) - (width // 2)
        y = (window.winfo_screenheight() // 2) - (height // 2)
        window.geometry(f"{width}x{height}+{x}+{y}")

    def open_image_viewer(self):
        # Создаем новое окно для просмотра изображений и логов
        self.image_window = tk.Toplevel(self.root)
        self.image_window.title("Просмотр изображений и логов")
        self.image_window.geometry("900x600")
        self.image_window.configure(bg="#f0f0f0")

        # Установка иконки
        try:
            self.image_window.iconbitmap("icon.ico")
        except tk.TclError:
            pass

        # Путь к папке (в будущем заменить на Google Drive)
        self.image_folder = r"C:\Users\user\Pictures"  # TODO: Интеграция с Google Drive
        self.image_extensions = (".png", ".jpg", ".jpeg", ".bmp", ".gif")
        self.log_extensions = (".txt",)

        # Получаем списки файлов
        self.image_files = []
        self.log_files = []
        if os.path.exists(self.image_folder):
            for ext in self.image_extensions:
                self.image_files.extend(glob.glob(os.path.join(self.image_folder, f"*{ext}")))
            for ext in self.log_extensions:
                self.log_files.extend(glob.glob(os.path.join(self.image_folder, f"*{ext}")))
        else:
            print(f"Ошибка: Папка {self.image_folder} не существует.")

        self.current_image_index = 0

        # Левый фрейм для изображений
        self.image_frame = tk.Frame(self.image_window, bg="#f0f0f0")
        self.image_frame.pack(side="left", fill="both", expand=True, padx=10, pady=10)

        # Метка для изображения
        self.image_label = tk.Label(self.image_frame, bg="#ffffff", bd=1, relief="solid")
        self.image_label.pack(pady=10, fill="both", expand=True)

        # Метка статуса
        self.status_label = tk.Label(
            self.image_frame,
            text="",
            font=("Helvetica", 10),
            bg="#f0f0f0"
        )
        self.status_label.pack(pady=5)

        # Кнопки для навигации
        self.button_frame = tk.Frame(self.image_frame, bg="#f0f0f0")
        self.button_frame.pack(pady=10)

        self.prev_button = tk.Button(
            self.button_frame,
            text="Предыдущее",
            command=self.show_previous_image,
            font=("Helvetica", 10),
            bg="#2196F3",
            fg="white",
            padx=10,
            relief="flat"
        )
        self.prev_button.pack(side="left", padx=5)

        self.next_button = tk.Button(
            self.button_frame,
            text="Следующее",
            command=self.show_next_image,
            font=("Helvetica", 10),
            bg="#2196F3",
            fg="white",
            padx=10,
            relief="flat"
        )
        self.next_button.pack(side="left", padx=5)

        # Правый фрейм для логов
        self.log_frame = tk.Frame(self.image_window, bg="#f0f0f0", width=300)
        self.log_frame.pack(side="right", fill="y", padx=10, pady=10)

        # Заголовок для логов
        tk.Label(
            self.log_frame,
            text="Логи",
            font=("Helvetica", 12, "bold"),
            bg="#f0f0f0"
        ).pack(anchor="w", pady=5)

        # Список логов
        self.log_listbox = tk.Listbox(
            self.log_frame,
            font=("Helvetica", 10),
            width=30,
            height=10,
            bd=1,
            relief="solid"
        )
        self.log_listbox.pack(fill="x", pady=5)
        self.log_listbox.bind("<<ListboxSelect>>", self.show_log_content)

        # Текстовое поле для содержимого лога
        self.log_text = tk.Text(
            self.log_frame,
            font=("Helvetica", 10),
            width=30,
            height=15,
            bd=1,
            relief="solid",
            state="disabled"
        )
        self.log_text.pack(fill="x", pady=5)

        # Заполняем список логов
        for log_file in self.log_files:
            self.log_listbox.insert(tk.END, os.path.basename(log_file))

        # Кнопка закрытия
        self.close_button = tk.Button(
            self.log_frame,
            text="Закрыть",
            command=self.image_window.destroy,
            font=("Helvetica", 10),
            bg="#f44336",
            fg="white",
            padx=10,
            relief="flat"
        )
        self.close_button.pack(pady=10)

        # Показываем первое изображение
        self.update_image()

    def update_image(self):
        if not self.image_files:
            self.image_label.configure(text=f"Изображения не найдены в папке {self.image_folder}")
            self.status_label.configure(text="")
            self.prev_button.configure(state="disabled")
            self.next_button.configure(state="disabled")
            return

        # Загружаем изображение
        image_path = self.image_files[self.current_image_index]
        try:
            image = Image.open(image_path)
            image.thumbnail((500, 400))
            photo = ImageTk.PhotoImage(image)
            self.image_label.configure(image=photo, text="")
            self.image_label.image = photo
        except Exception as e:
            self.image_label.configure(text=f"Ошибка загрузки: {image_path}\n{e}")
            print(f"Ошибка загрузки изображения {image_path}: {e}")

        # Обновляем статус
        self.status_label.configure(
            text=f"Изображение {self.current_image_index + 1} из {len(self.image_files)}"
        )

        # Обновляем кнопки
        self.prev_button.configure(state="normal" if self.current_image_index > 0 else "disabled")
        self.next_button.configure(
            state="normal" if self.current_image_index < len(self.image_files) - 1 else "disabled"
        )

    def show_previous_image(self):
        if self.current_image_index > 0:
            self.current_image_index -= 1
            self.update_image()

    def show_next_image(self):
        if self.current_image_index < len(self.image_files) - 1:
            self.current_image_index += 1
            self.update_image()

    def show_log_content(self, event):
        selection = self.log_listbox.curselection()
        if not selection:
            return
        index = selection[0]
        log_file = self.log_files[index]
        try:
            with open(log_file, "r", encoding="utf-8") as f:
                content = f.read()
            self.log_text.configure(state="normal")
            self.log_text.delete(1.0, tk.END)
            self.log_text.insert(tk.END, content)
            self.log_text.configure(state="disabled")
        except Exception as e:
            self.log_text.configure(state="normal")
            self.log_text.delete(1.0, tk.END)
            self.log_text.insert(tk.END, f"Ошибка чтения: {e}")
            self.log_text.configure(state="disabled")

if __name__ == "__main__":
    root = tk.Tk()
    app = WelcomeApp(root)
    root.mainloop()