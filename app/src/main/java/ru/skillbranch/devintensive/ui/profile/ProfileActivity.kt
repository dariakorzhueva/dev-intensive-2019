package ru.skillbranch.devintensive.ui.profile

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_profile.*
import ru.skillbranch.devintensive.R
import ru.skillbranch.devintensive.models.Profile
import ru.skillbranch.devintensive.utils.Utils
import ru.skillbranch.devintensive.utils.Utils.isValidRepositoryPath
import ru.skillbranch.devintensive.viewmodels.ProfileViewModel
import kotlin.math.roundToInt


class ProfileActivity : AppCompatActivity() {
    companion object {
        const val IS_EDIT_MODE = "IS_EDIT_MODE"
    }

    private lateinit var viewModel: ProfileViewModel
    var isEditMode = false
    var isValidRepository = true;
    lateinit var viewFields: Map<String, TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        initViews(savedInstanceState)
        initViewModel()
        Log.d("M_ProfileActivity", "onCreate")
    }

    override fun onResume() {
        super.onResume()

        et_repository.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (!isValidRepositoryPath(s.toString())) {
                    wr_repository.error = "Невалидный адрес репозитория"
                    isValidRepository = false
                } else {
                    isValidRepository = true
                    wr_repository.isErrorEnabled = false
                }
            }
        })
    }

    /*override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putBoolean(IS_EDIT_MODE, isEditMode)

    }*/

    private fun initViewModel() {
        /* Получение класса для ViewModel */
        viewModel = ViewModelProviders.of(this).get(ProfileViewModel::class.java)
        viewModel.getProfileData().observe(this, Observer { updateUI(it) })
        viewModel.getTheme().observe(this, Observer { updateTheme(it) })
    }

    private fun updateTheme(mode: Int) {
        Log.d("M_ProfileActivity", "updateTheme")
        delegate.setLocalNightMode(mode)
    }

    private fun updateUI(profile: Profile) {
        profile.toMap().also {
            for ((k, v) in viewFields) {
                v.text = it[k].toString()
            }

            val initials = Utils.toInitials(it["firstName"].toString(), it["lastName"].toString()) ?: ""
            drawDefaultAvatar(initials)
        }
    }

    /* Инициализация views */
    private fun initViews(savedInstanceState: Bundle?) {
        /* Карта, связывающая ключ и представление */
        viewFields = mapOf(
            "nickName" to tv_nick_name,
            "rank" to tv_rank,
            "firstName" to et_first_name,
            "lastName" to et_last_name,
            "about" to et_about,
            "repository" to et_repository,
            "rating" to tv_rating,
            "respect" to tv_respect
        )

        isEditMode = savedInstanceState?.getBoolean(IS_EDIT_MODE, false) ?: false
        showCurerentMode(isEditMode)

        btn_edit.setOnClickListener {
            /* Сохранение информации в профиль */
            if (isEditMode) {
                if (!isValidRepository) {
                    et_repository.text.clear()
                    wr_repository.isErrorEnabled = false
                }

                saveProfileInfo()
            }

            isEditMode = !isEditMode
            showCurerentMode(isEditMode)
        }

        btn_switch_theme.setOnClickListener {
            viewModel.switchTheme()
        }
    }

    /* Отобразить текущий режим */
    private fun showCurerentMode(isEdit: Boolean) {
        val info = viewFields.filter { setOf("firstName", "lastName", "about", "repository").contains(it.key) }

        for ((_, v) in info) {
            v as EditText
            v.isFocusable = isEdit
            v.isFocusableInTouchMode = isEdit
            v.isEnabled = isEdit
            v.background.alpha = if (isEdit) 255 else 0
        }

        /* Исчезновение иконки глаза в режиме редактирования */
        ic_eye.visibility = if (isEdit) View.GONE else View.VISIBLE

        wr_about.isCounterEnabled = isEdit

        with(btn_edit) {
            val filter: ColorFilter? = if (isEdit) {
                PorterDuffColorFilter(
                    resources.getColor(R.color.color_accent, theme),
                    PorterDuff.Mode.SRC_IN
                )
            } else {
                null
            }

            val icon = if (isEdit) {
                resources.getDrawable(R.drawable.ic_save_black_24dp, theme)
            } else {
                resources.getDrawable(R.drawable.ic_edit_black_24dp, theme)
            }

            background.colorFilter = filter
            setImageDrawable(icon)
        }
    }

    /* Сохранение данных */
    private fun saveProfileInfo() {
        Profile(
            firstName = et_first_name.text.toString(),
            lastName = et_last_name.text.toString(),
            about = et_about.text.toString(),
            repository = et_repository.text.toString()
        ).apply {
            viewModel.saveProfileData(this)
        }
    }

    /* Аватар по умолчанию */
    private fun drawDefaultAvatar(initials: String, textSize: Float = 48f, color: Int = Color.WHITE) {
        val bitmap = convertTextToBitmap(initials, textSize, color)
        val drawable = BitmapDrawable(resources, bitmap)

        iv_avatar.setImageDrawable(drawable)
    }

    /* Преобразование текста в Bitmap */
    private fun convertTextToBitmap(text: String, textSize: Float, textColor: Int): Bitmap {
        val dp = resources.displayMetrics.density.roundToInt()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = textSize*dp
        paint.color = textColor
        paint.textAlign = Paint.Align.CENTER

        val image = Bitmap.createBitmap(112*dp, 112*dp, Bitmap.Config.ARGB_8888)

        val value = TypedValue()
        this.theme.resolveAttribute(R.attr.colorAccent, value, true)
        image.eraseColor(value.data)

        val canvas = Canvas(image)
        canvas.drawText(text, 56f*dp, 56f*dp + paint.textSize/3, paint)

        return image
    }
}

