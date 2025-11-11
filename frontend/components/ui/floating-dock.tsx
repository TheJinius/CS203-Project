"use client"

import { cn } from "@/lib/utils"
import { IconLayoutNavbarCollapse } from "@tabler/icons-react"
import {
  AnimatePresence,
  MotionValue,
  motion,
  useMotionValue,
  useSpring,
  useTransform,
} from "framer-motion"
import Link from "next/link"
import { useRef, useState } from "react"

export const FloatingDock = ({
  items,
  desktopClassName,
  mobileClassName,
}: {
  items: { title: string; icon: React.ReactNode; href?: string; onClick?: () => void }[]
  desktopClassName?: string
  mobileClassName?: string
}) => {
  return (
    <>
      <FloatingDockDesktop items={items} className={desktopClassName} />
      <FloatingDockMobile items={items} className={mobileClassName} />
    </>
  )
}

const FloatingDockMobile = ({
  items,
  className,
}: {
  items: { title: string; icon: React.ReactNode; href?: string; onClick?: () => void }[]
  className?: string
}) => {
  const [open, setOpen] = useState(false)
  return (
    <div className={cn("relative block md:hidden", className)}>
      <AnimatePresence>
        {open && (
          <motion.div
            layoutId="nav"
            className="absolute bottom-full mb-2 inset-x-0 flex flex-col gap-2"
          >
            {items.map((item, idx) => (
              <motion.div
                key={item.title}
                initial={{ opacity: 0, y: 10 }}
                animate={{
                  opacity: 1,
                  y: 0,
                }}
                exit={{
                  opacity: 0,
                  y: 10,
                  transition: {
                    delay: idx * 0.05,
                  },
                }}
                transition={{ delay: (items.length - 1 - idx) * 0.05 }}
              >
                {item.onClick ? (
                  <button
                    onClick={() => {
                      item.onClick?.()
                      setOpen(false)
                    }}
                    className="flex items-center gap-2 px-4 py-2 rounded-lg bg-gray-50 dark:bg-neutral-900"
                  >
                    <div className="h-4 w-4">{item.icon}</div>
                    <span className="text-sm font-medium text-neutral-700 dark:text-neutral-200">
                      {item.title}
                    </span>
                  </button>
                ) : (
                  <Link
                    href={item.href || "#"}
                    key={item.title}
                    className="flex items-center gap-2 px-4 py-2 rounded-lg bg-gray-50 dark:bg-neutral-900"
                  >
                    <div className="h-4 w-4">{item.icon}</div>
                    <span className="text-sm font-medium text-neutral-700 dark:text-neutral-200">
                      {item.title}
                    </span>
                  </Link>
                )}
              </motion.div>
            ))}
          </motion.div>
        )}
      </AnimatePresence>
      <button
        onClick={() => setOpen(!open)}
        className="h-10 w-10 rounded-full bg-gray-50 dark:bg-neutral-800 flex items-center justify-center"
      >
        <IconLayoutNavbarCollapse className="h-5 w-5 text-neutral-500 dark:text-neutral-400" />
      </button>
    </div>
  )
}

const FloatingDockDesktop = ({
  items,
  className,
}: {
  items: { title: string; icon: React.ReactNode; href?: string; onClick?: () => void }[]
  className?: string
}) => {
  const mouseX = useMotionValue(Infinity)
  return (
    <motion.div
      onMouseMove={(e) => mouseX.set(e.pageX)}
      onMouseLeave={() => mouseX.set(Infinity)}
      className={cn(
        "mx-auto hidden md:flex h-16 gap-2 items-center rounded-2xl bg-white dark:bg-slate-800 px-4 py-3 border border-slate-200 dark:border-slate-700",
        className
      )}
    >
      {items.map((item) => (
        <IconContainer mouseX={mouseX} key={item.title} {...item} />
      ))}
    </motion.div>
  )
}

function IconContainer({
  mouseX,
  title,
  icon,
  href,
  onClick,
}: {
  mouseX: MotionValue
  title: string
  icon: React.ReactNode
  href?: string
  onClick?: () => void
}) {
  const ref = useRef<HTMLDivElement>(null)

  const distance = useTransform(mouseX, (val) => {
    const bounds = ref.current?.getBoundingClientRect() ?? { x: 0, width: 0 }
    return val - bounds.x - bounds.width / 2
  })

  const scaleTransform = useTransform(distance, [-150, 0, 150], [1, 1.1, 1])

  const scale = useSpring(scaleTransform, {
    mass: 0.1,
    stiffness: 150,
    damping: 12,
  })

  const [hovered, setHovered] = useState(false)

  if (onClick) {
    return (
      <motion.div
        ref={ref}
        style={{ scale }}
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
        className="flex items-center gap-2 px-3 py-2 rounded-xl bg-gray-200 dark:bg-neutral-800 hover:bg-gray-300 dark:hover:bg-neutral-700 cursor-pointer transition-colors relative"
        onClick={onClick}
      >
        <div className="h-5 w-5 flex-shrink-0">{icon}</div>
        <span className="text-sm font-medium text-neutral-700 dark:text-neutral-200 whitespace-nowrap">
          {title}
        </span>
      </motion.div>
    )
  }

  return (
    <Link href={href || "#"}>
      <motion.div
        ref={ref}
        style={{ scale }}
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
        className="flex items-center gap-2 px-3 py-2 rounded-xl bg-gray-200 dark:bg-neutral-800 hover:bg-gray-300 dark:hover:bg-neutral-700 transition-colors relative"
      >
        <div className="h-5 w-5 flex-shrink-0">{icon}</div>
        <span className="text-sm font-medium text-neutral-700 dark:text-neutral-200 whitespace-nowrap">
          {title}
        </span>
      </motion.div>
    </Link>
  )
}
