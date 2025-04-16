import tkinter as tk
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
        self.root.geometry("400x200")
        self.root.configure(bg="#f0f0f0")

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

        # Создаем и размещаем элементы интерфейса
        self.label = tk.Label(
            root,
            text=f"{greeting}, {user_name}!\nДобро пожаловать!",
            font=("Arial", 14),
            bg="#f0f0f0",
            pady=20
        )
        self.label.pack(expand=True)

        # Кнопка "Старт"
        self.start_button = tk.Button(
            root,
            text="Старт",
            command=self.open_image_viewer,
            font=("Arial", 10),
            bg="#4CAF50",
            fg="white",
            padx=10
        )
        self.start_button.pack(pady=10)

        # Центрируем окно на экране
        self.center_window()

    def center_window(self):
        self.root.update_idletasks()
        width = self.root.winfo_width()
        height = self.root.winfo_height()
        x = (self.root.winfo_screenwidth() // 2) - (width // 2)
        y = (self.root.winfo_screenheight() // 2) - (height // 2)
        self.root.geometry(f"{width}x{height}+{x}+{y}")

    def open_image_viewer(self):
        # Создаем новое окно для просмотра изображений
        self.image_window = tk.Toplevel(self.root)
        self.image_window.title("Просмотр изображений")
        self.image_window.geometry("600x500")
        self.image_window.configure(bg="#f0f0f0")

        # Установка иконки для нового окна
        try:
            self.image_window.iconbitmap("icon.ico")
        except tk.TclError:
            pass

        # Путь к папке с изображениями (измените на нужный путь)
        self.image_folder = 'C:\\Users\\user\\Pictures'
        self.image_extensions = (".png", ".jpg", ".jpeg", ".bmp", ".gif")

        # Получаем список изображений
        self.image_files = []
        if os.path.exists(self.image_folder):
            for ext in self.image_extensions:
                self.image_files.extend(glob.glob(os.path.join(self.image_folder, f"*{ext}")))
        else:
            self.image_files = []

        self.current_image_index = 0

        # Метка для отображения изображения
        self.image_label = tk.Label(self.image_window, bg="#f0f0f0")
        self.image_label.pack(pady=10, expand=True)

        # Метка для статуса (например, "Изображение 1 из 5")
        self.status_label = tk.Label(
            self.image_window,
            text="",
            font=("Arial", 10),
            bg="#f0f0f0"
        )
        self.status_label.pack(pady=5)

        # Фрейм для кнопок
        self.button_frame = tk.Frame(self.image_window, bg="#f0f0f0")
        self.button_frame.pack(pady=10)

        # Кнопка "Предыдущее"
        self.prev_button = tk.Button(
            self.button_frame,
            text="Предыдущее",
            command=self.show_previous_image,
            font=("Arial", 10),
            bg="#2196F3",
            fg="white",
            padx=10
        )
        self.prev_button.pack(side=tk.LEFT, padx=5)

        # Кнопка "Следующее"
        self.next_button = tk.Button(
            self.button_frame,
            text="Следующее",
            command=self.show_next_image,
            font=("Arial", 10),
            bg="#2196F3",
            fg="white",
            padx=10
        )
        self.next_button.pack(side=tk.LEFT, padx=5)

        # Кнопка "Закрыть"
        self.close_button = tk.Button(
            self.button_frame,
            text="Закрыть",
            command=self.image_window.destroy,
            font=("Arial", 10),
            bg="#f44336",
            fg="white",
            padx=10
        )
        self.close_button.pack(side=tk.LEFT, padx=5)

        # Показываем первое изображение
        self.update_image()

    def update_image(self):
        if not self.image_files:
            self.image_label.configure(text="Изображения не найдены в папке 'images'")
            self.status_label.configure(text="")
            self.prev_button.configure(state="disabled")
            self.next_button.configure(state="disabled")
            return

        # Загружаем текущее изображение
        image_path = self.image_files[self.current_image_index]
        try:
            image = Image.open(image_path)
            # Масштабируем изображение, чтобы оно помещалось в окно
            image.thumbnail((550, 400))
            photo = ImageTk.PhotoImage(image)
            self.image_label.configure(image=photo, text="")
            self.image_label.image = photo  # Сохраняем ссылку, чтобы избежать сборки мусора
        except Exception as e:
            self.image_label.configure(text=f"Ошибка загрузки: {image_path}")

        # Обновляем статус
        self.status_label.configure(
            text=f"Изображение {self.current_image_index + 1} из {len(self.image_files)}"
        )

        # Обновляем состояние кнопок
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


if __name__ == "__main__":
    root = tk.Tk()
    app = WelcomeApp(root)
    root.mainloop()